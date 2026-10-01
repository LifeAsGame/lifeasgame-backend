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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(RoleEventLifeLogIntegrationTest.PlayerIdentityConfig.class)
@RecordApplicationEvents
@DisplayName("활성 RoleEvent HTTP·MySQL와 과거 LifeLog 연결 계약")
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
        // Existing event remains linked to an explicitly authored LifeLog.
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

    @Test
    @DisplayName("생성·수정·참가자 변경·완료와 별도 취소를 저장하고 LifeLog는 자동 생성하지 않는다")
    void persistsLifecycleWithoutLifeLogSideEffects() throws Exception {
        long lifeLogCount = jdbc.queryForObject("SELECT COUNT(*) FROM life_log_records WHERE player_id = ?", Long.class, PLAYER_ID);
        long outboxCount = jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class);
        jdbc.update("""
                INSERT INTO persons (owner_player_id, display_name, status, version, created_at, updated_at)
                VALUES (?, '동료', 'ACTIVE', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID);
        long personId = jdbc.queryForObject("SELECT MAX(id) FROM persons WHERE owner_player_id = ?", Long.class, PLAYER_ID);
        jdbc.update("""
                INSERT INTO persons (owner_player_id, display_name, status, version, created_at, updated_at)
                VALUES (?, '타인 Person', 'ACTIVE', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID + 1);
        long foreignPersonId = jdbc.queryForObject("SELECT MAX(id) FROM persons WHERE owner_player_id = ?", Long.class, PLAYER_ID + 1);
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, nickname, status, created_at, updated_at)
                VALUES (?, 'role-event-fixture@example.test', 'hash', 'fixture', 'ACTIVE', NOW(6), NOW(6))
                """, USER_ID + 1);
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        String base = "/api/v1/roles/" + roleId + "/events";
        String token = "Bearer " + jwtProvider.createAccessToken(USER_ID, PLAYER_ID);

        String created = mockMvc.perform(post(base).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"새 일정\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.status").value("PLANNED"))
                .andReturn().getResponse().getContentAsString();
        long newId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).path("result").path("id").asLong();
        String detail = base + "/" + newId;
        mockMvc.perform(patch(detail).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"수정 일정\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.title").value("수정 일정"));
        String added = mockMvc.perform(post(detail + "/participants").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantType\":\"PERSON\",\"participantId\":" + personId + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long linkId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(added).path("result").path("participantLinkId").asLong();
        mockMvc.perform(post(detail + "/participants").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantType\":\"PERSON\",\"participantId\":" + personId + "}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(detail + "/participants").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantType\":\"PERSON\",\"participantId\":" + foreignPersonId + "}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PER-404-NOT-FOUND"));
        mockMvc.perform(post(detail + "/participants").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantType\":\"SERVICE_USER\",\"participantId\":" + (USER_ID + 1) + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.participantType").value("SERVICE_USER"));
        mockMvc.perform(delete(detail + "/participants/" + linkId).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(detail + "/complete").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.completedAt").isNotEmpty());
        mockMvc.perform(post(detail + "/complete").header("Authorization", token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ROL-409-EVENT-NOT-PLANNED"));
        mockMvc.perform(patch(detail).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"늦은 수정\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get(detail).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("COMPLETED"));
        mockMvc.perform(post(base).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"취소 일정\"}"))
                .andExpect(status().isCreated());
        long cancelId = jdbc.queryForObject("SELECT MAX(id) FROM role_events WHERE player_id = ?", Long.class, PLAYER_ID);
        mockMvc.perform(post(base + "/" + cancelId + "/cancel").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("CANCELED"));
        mockMvc.perform(post(base + "/" + cancelId + "/complete").header("Authorization", token))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM life_log_records WHERE player_id = ?", Long.class, PLAYER_ID))
                .isEqualTo(lifeLogCount);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class)).isEqualTo(outboxCount);
        verifyNoInteractions(publisher);
        assertThat(events.stream(DomainEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("동시 완료·취소에서 한 상태 전이만 성공한다")
    void serializesTerminalTransition() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        String path = "/api/v1/roles/" + roleId + "/events/" + eventId;
        String token = "Bearer " + jwtProvider.createAccessToken(USER_ID, PLAYER_ID);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var complete = pool.submit(() -> {
                start.await();
                return mockMvc.perform(post(path + "/complete").header("Authorization", token))
                        .andReturn().getResponse().getStatus();
            });
            var cancel = pool.submit(() -> {
                start.await();
                return mockMvc.perform(post(path + "/cancel").header("Authorization", token))
                        .andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(List.of(complete.get(15, TimeUnit.SECONDS), cancel.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT status FROM role_events WHERE id = ?", String.class, eventId))
                .isIn("COMPLETED", "CANCELED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM life_log_records WHERE player_id = ?", Long.class, PLAYER_ID))
                .isEqualTo(1);
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
        String code = endpoint == Endpoint.DETAIL
                ? "ROL-404-EVENT-NOT-FOUND" : "ROL-404-NOT-FOUND";
        mockMvc.perform(authenticated(endpoint, PLAYER_ID + 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(code));
        assertUnchanged(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLANNED", "COMPLETED", "CANCELED"})
    @DisplayName("보관 Role의 과거 Event는 조회하고 모든 command는 409로 차단한다")
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
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ROL-409-ARCHIVED"));
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
