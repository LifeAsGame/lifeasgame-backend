package online.lifeasgame.role.application;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(RoleEventLifeLogIntegrationTest.PlayerIdentityConfig.class)
@RecordApplicationEvents
@DisplayName("GATED RoleEvent HTTP·MySQL와 과거 LifeLog 연결 계약")
class RoleEventLifeLogIntegrationTest {

    private static final long PLAYER_ID = 25201L;
    private static final long USER_ID = 25202L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_role_event_lifelog")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired private LifeLogRecordRegistrar registrar;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ApplicationEvents events;
    @MockitoBean private UserAuthApi userAuthApi;
    @MockitoSpyBean private DomainEventPublisher publisher;

    private long roleId;
    private long eventId;
    private long participantId;

    enum Endpoint { CREATE, UPDATE, COMPLETE, CANCEL, ADD_PARTICIPANT, REMOVE_PARTICIPANT, LIST, DETAIL }

    @BeforeEach
    void seedHistory() {
        jdbc.update("DELETE FROM life_log_records WHERE player_id = ?", PLAYER_ID);
        jdbc.update("DELETE FROM role_event_participants");
        jdbc.update("DELETE FROM role_events WHERE player_id = ?", PLAYER_ID);
        jdbc.update("DELETE FROM roles WHERE player_id = ?", PLAYER_ID);
        jdbc.update("""
                INSERT INTO roles (player_id, role_type, name, status, created_at, updated_at, version)
                VALUES (?, 'WORK', 'Developer', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 0)
                """, PLAYER_ID);
        roleId = jdbc.queryForObject("SELECT id FROM roles WHERE player_id = ?", Long.class, PLAYER_ID);
        // Historical data is seeded independently of the now-gated public command API.
        jdbc.update("""
                INSERT INTO role_events (player_id, role_id, title, description, status, version, created_at, updated_at)
                VALUES (?, ?, '팀 회고', '과거 기록', 'PLANNED', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID, roleId);
        eventId = jdbc.queryForObject("SELECT id FROM role_events WHERE player_id = ?", Long.class, PLAYER_ID);
        jdbc.update("""
                INSERT INTO role_event_participants (role_event_id, participant_type, participant_id, created_at, updated_at)
                VALUES (?, 'PERSON', 3, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, eventId);
        participantId = jdbc.queryForObject("SELECT id FROM role_event_participants WHERE role_event_id = ?", Long.class, eventId);
        registerLifeLog();
        clearInvocations(publisher);
        events.clear();
    }

    @ParameterizedTest
    @EnumSource(value = Endpoint.class, names = {"LIST", "DETAIL"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("소유자의 모든 command는 403이며 영속 상태와 이벤트 발행이 불변이다")
    void rejectsOwnedCommandWithoutSideEffects(Endpoint endpoint) throws Exception {
        var before = snapshot();

        mockMvc.perform(authenticated(endpoint, PLAYER_ID))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ROL-403-EVENT-COMMAND-GATED"))
                .andExpect(jsonPath("$.title").value("Role event commands are not available"))
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty())
                .andExpect(jsonPath("$.result").doesNotExist());

        assertUnchanged(before);
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    @DisplayName("비인증 요청은 모든 read·command에서 401이며 상태가 불변이다")
    void rejectsAnonymous(Endpoint endpoint) throws Exception {
        var before = snapshot();
        mockMvc.perform(request(endpoint)).andExpect(status().isUnauthorized());
        assertUnchanged(before);
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    @DisplayName("타인 소유 대상은 모든 read·command에서 404이며 상태가 불변이다")
    void rejectsForeignOwner(Endpoint endpoint) throws Exception {
        var before = snapshot();
        String code = endpoint == Endpoint.CREATE || endpoint == Endpoint.LIST
                ? "ROL-404-NOT-FOUND" : "ROL-404-EVENT-NOT-FOUND";
        mockMvc.perform(authenticated(endpoint, PLAYER_ID + 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(code));
        assertUnchanged(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLANNED", "COMPLETED", "CANCELED"})
    @DisplayName("과거 Event의 모든 상태에서 목록·상세·참가자 조회는 허용하고 command는 차단한다")
    void preservesHistoricalReads(String eventStatus) throws Exception {
        jdbc.update("UPDATE role_events SET status = ?, completed_at = ? WHERE id = ?", eventStatus,
                eventStatus.equals("COMPLETED") ? java.sql.Timestamp.valueOf("2026-08-11 03:00:00") : null, eventId);
        jdbc.update("UPDATE roles SET status = 'ARCHIVED' WHERE id = ?", roleId);
        var before = snapshot();

        mockMvc.perform(authenticated(Endpoint.LIST, PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(eventId))
                .andExpect(jsonPath("$.result[0].status").value(eventStatus))
                .andExpect(jsonPath("$.result[0].participants[0].participantLinkId").value(participantId));
        mockMvc.perform(authenticated(Endpoint.DETAIL, PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(eventId))
                .andExpect(jsonPath("$.result.status").value(eventStatus))
                .andExpect(jsonPath("$.result.participants[0].participantType").value("PERSON"))
                .andExpect(jsonPath("$.result.participants[0].participantId").value(3));
        for (Endpoint endpoint : List.of(Endpoint.CREATE, Endpoint.UPDATE, Endpoint.COMPLETE,
                Endpoint.CANCEL, Endpoint.ADD_PARTICIPANT, Endpoint.REMOVE_PARTICIPANT)) {
            mockMvc.perform(authenticated(endpoint, PLAYER_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ROL-403-EVENT-COMMAND-GATED"));
        }
        assertUnchanged(before);
    }

    @Test
    @DisplayName("기존 Event의 명시적 LifeLog 연결은 Role을 derive해 원자적으로 유지한다")
    void persistsDerivedRoleContext() {
        assertThat(jdbc.queryForMap("""
                SELECT primary_role_id, role_event_id FROM life_log_records WHERE player_id = ?
                """, PLAYER_ID))
                .containsEntry("primary_role_id", roleId)
                .containsEntry("role_event_id", eventId);
    }

    private LifeLogRecord registerLifeLog() {
        return new TransactionTemplate(transactionManager).execute(status -> registrar.register(
                PLAYER_ID, LifeLogSourceType.COLLECTION, 252001L, LifeLogEntryMode.FULL,
                new LifeLogRecordMetadataCommand(null, null, null, eventId)));
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : List.of("roles", "role_events", "role_event_participants", "persons",
                "role_relations", "life_log_records", "outbox_events")) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        return result;
    }

    private void assertUnchanged(Map<String, List<Map<String, Object>>> before) {
        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(publisher);
        assertThat(events.stream(DomainEvent.class)).isEmpty();
    }

    private MockHttpServletRequestBuilder authenticated(Endpoint endpoint, long playerId) {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        return request(endpoint).header("Authorization", "Bearer " + jwtProvider.createAccessToken(USER_ID, playerId));
    }

    private MockHttpServletRequestBuilder request(Endpoint endpoint) {
        String collection = "/api/v1/roles/" + roleId + "/events";
        String detail = collection + "/" + eventId;
        return switch (endpoint) {
            case CREATE -> post(collection).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"새 사건\"}");
            case UPDATE -> patch(detail).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"변경\"}");
            case COMPLETE -> post(detail + "/complete");
            case CANCEL -> post(detail + "/cancel");
            case ADD_PARTICIPANT -> post(detail + "/participants").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"participantType\":\"PERSON\",\"participantId\":4}");
            case REMOVE_PARTICIPANT -> delete(detail + "/participants/" + participantId);
            case LIST -> get(collection);
            case DETAIL -> get(detail);
        };
    }

    @TestConfiguration
    static class PlayerIdentityConfig {
        @Bean
        @Primary
        CurrentPlayerAccessor roleEventTestPlayerAccessor() {
            return new JwtCurrentPlayerAccessor();
        }
    }
}
