package online.lifeasgame.social.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(RolePartyIntegrationTest.IdentityConfig.class)
@DisplayName("RoleParty 초대형 모임의 HTTP·MySQL 권한과 경쟁 계약")
class RolePartyIntegrationTest {
    private static final long LEADER = 61201L;
    private static final long A = 61202L;
    private static final long B = 61203L;
    private static final long OUTSIDER = 61204L;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_role_party")
            .withUsername("lifeasgame").withPassword("lifeasgame");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider jwt;
    @Autowired private ObjectMapper json;
    @MockitoBean private UserAuthApi userAuthApi;
    private long roleId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM role_party_invitations");
        jdbc.update("DELETE FROM role_party_members");
        jdbc.update("DELETE FROM role_parties");
        jdbc.update("DELETE FROM follows");
        jdbc.update("DELETE FROM roles WHERE player_id = ?", LEADER);
        jdbc.update("DELETE FROM player WHERE id BETWEEN ? AND ?", LEADER, OUTSIDER);
        given(userAuthApi.resolveAuthorization(any())).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        for (long id : List.of(LEADER, A, B, OUTSIDER)) {
            jdbc.update("""
                    INSERT INTO player (id, user_id, name, level, exp, hp_cur, hp_cap, mp_cur, mp_cap,
                        str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat, extra_stats,
                        status_effects, version, created_at, updated_at)
                    VALUES (?, ?, 'fixture', 1, 0, 100, 100, 50, 50, 1, 1, 1, 1, 1, 1,
                        JSON_OBJECT(), '[]', 0, NOW(6), NOW(6))
                    """, id, id);
        }
        jdbc.update("""
                INSERT INTO roles (player_id, role_type, name, status, created_at, updated_at, version)
                VALUES (?, 'WORK', 'private fixture', 'ACTIVE', NOW(6), NOW(6), 0)
                """, LEADER);
        roleId = jdbc.queryForObject("SELECT MAX(id) FROM roles WHERE player_id = ?", Long.class, LEADER);
        for (long peer : List.of(A, B)) {
            jdbc.update("""
                    INSERT INTO follows (player_id, target_player_id, state, blocked, muted, created_at, updated_at)
                    VALUES (?, ?, 'FOLLOWING', false, false, NOW(6), NOW(6))
                    """, LEADER, peer);
            jdbc.update("""
                    INSERT INTO follows (player_id, target_player_id, state, blocked, muted, created_at, updated_at)
                    VALUES (?, ?, 'FOLLOWING', false, false, NOW(6), NOW(6))
                    """, peer, LEADER);
        }
    }

    @Test
    @DisplayName("생성·초대·마지막 한 자리 동시 수락에서 한 명만 가입하고 외부인은 404다")
    void serializesLastSeatAndHidesPrivateRole() throws Exception {
        String created = mvc.perform(auth(post("/api/v1/roles/" + roleId + "/role-parties"), LEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"작은 모임\",\"description\":\"공유 소개\",\"maxMembers\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.memberCount").value(1))
                .andReturn().getResponse().getContentAsString();
        long partyId = result(created).path("id").asLong();
        String base = "/api/v1/role-parties/" + partyId;
        mvc.perform(auth(get(base), OUTSIDER)).andExpect(status().isNotFound());
        mvc.perform(auth(get(base), A)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/roles/" + roleId + "/role-parties"), A))
                .andExpect(status().isNotFound());

        long firstInvite = invite(base, A);
        long secondInvite = invite(base, B);
        assertThat(invite(base, A)).isEqualTo(firstInvite);
        mvc.perform(auth(get("/api/v1/role-parties/invitations/mine"), A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].groupName").value("작은 모임"))
                .andExpect(jsonPath("$.result.contents[0].roleId").doesNotExist());
        mvc.perform(auth(get(base), A)).andExpect(status().isNotFound());

        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { start.await(); return mvc.perform(auth(post(base + "/invitations/" + firstInvite + "/accept"), A)).andReturn().getResponse().getStatus(); });
            var b = pool.submit(() -> { start.await(); return mvc.perform(auth(post(base + "/invitations/" + secondInvite + "/accept"), B)).andReturn().getResponse().getStatus(); });
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_party_members WHERE role_party_id = ? AND left_at IS NULL", Long.class, partyId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_party_invitations WHERE role_party_id = ? AND status = 'ACCEPTED'", Long.class, partyId)).isEqualTo(1);
        mvc.perform(auth(get(base), OUTSIDER)).andExpect(status().isNotFound());
        mvc.perform(auth(get(base), LEADER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.roleId").doesNotExist());
        mvc.perform(auth(post(base + "/leave"), LEADER)).andExpect(status().isConflict());
        mvc.perform(auth(post(base + "/disband"), LEADER)).andExpect(status().isOk());
        mvc.perform(auth(post(base + "/invitations"), LEADER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inviteePlayerId\":" + A + "}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_party_members WHERE role_party_id = ?", Long.class, partyId)).isEqualTo(2);
    }

    @Test
    @DisplayName("초대 결정·리더 이전·탈퇴·Role 보관·해산을 이력 손실 없이 처리한다")
    void preservesIndependentLifecycle() throws Exception {
        String created = mvc.perform(auth(post("/api/v1/roles/" + roleId + "/role-parties"), LEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"모임\",\"maxMembers\":3}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long partyId = result(created).path("id").asLong();
        String base = "/api/v1/role-parties/" + partyId;
        long inviteA = invite(base, A);
        mvc.perform(auth(post(base + "/invitations/" + inviteA + "/decline"), A))
                .andExpect(status().isNoContent());
        assertThat(invite(base, A)).isEqualTo(inviteA);
        mvc.perform(auth(post(base + "/invitations/" + inviteA + "/accept"), A))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.memberCount").value(2));
        mvc.perform(auth(post(base + "/invitations/" + inviteA + "/accept"), A))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.memberCount").value(2));
        long inviteB = invite(base, B);
        mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        base + "/invitations/" + inviteB), LEADER)).andExpect(status().isNoContent());
        mvc.perform(auth(post(base + "/invitations/" + inviteB + "/accept"), B))
                .andExpect(status().isConflict());
        mvc.perform(auth(patch(base), LEADER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"변경한 모임\",\"description\":\"공유 설명\",\"maxMembers\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.name").value("변경한 모임"));
        mvc.perform(auth(post(base + "/transfer-leader"), LEADER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"toPlayerId\":" + A + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.leaderPlayerId").value(A));
        mvc.perform(auth(post(base + "/disband"), LEADER)).andExpect(status().isNotFound());
        mvc.perform(auth(post(base + "/leave"), LEADER)).andExpect(status().isNoContent());
        mvc.perform(auth(get(base), LEADER)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/role-parties/mine"), LEADER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].group.id").value(partyId))
                .andExpect(jsonPath("$.result.contents[0].membershipStatus").value("LEFT"));
        jdbc.update("UPDATE roles SET status = 'ARCHIVED' WHERE id = ?", roleId);
        mvc.perform(auth(post("/api/v1/roles/" + roleId + "/role-parties"), LEADER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 모임\",\"maxMembers\":2}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(get(base), A)).andExpect(status().isOk());
        mvc.perform(auth(post(base + "/disband"), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DISBANDED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_party_members WHERE role_party_id = ?", Long.class, partyId)).isEqualTo(2);
    }

    private long invite(String base, long target) throws Exception {
        String body = mvc.perform(auth(post(base + "/invitations"), LEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteePlayerId\":" + target + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return result(body).path("invitationId").asLong();
    }

    private JsonNode result(String body) throws Exception { return json.readTree(body).path("result"); }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, long playerId) {
        return request.header("Authorization", "Bearer " + jwt.createAccessToken(playerId, playerId));
    }

    @TestConfiguration
    static class IdentityConfig {
        @Bean @Primary CurrentPlayerAccessor rolePartyPlayerAccessor() { return new JwtCurrentPlayerAccessor(); }
    }
}
