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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(GuildGroupsEventsIntegrationTest.IdentityConfig.class)
@DisplayName("Guild 연결과 공유 행사의 HTTP·MySQL 계약")
class GuildGroupsEventsIntegrationTest {
    private static final long A = 62101L, B = 62102L, C = 62103L, D = 62104L;
    private static final long GUILD = 62101L, PARTY = 62102L;

    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_guild_groups_events")
            .withUsername("lifeasgame").withPassword("lifeasgame");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired ObjectMapper json;
    @MockitoBean UserAuthApi userAuthApi;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM guild_event_rsvps");
        jdbc.update("DELETE FROM guild_events");
        jdbc.update("DELETE FROM guild_group_links");
        jdbc.update("DELETE FROM role_party_invitations");
        jdbc.update("DELETE FROM role_party_members");
        jdbc.update("DELETE FROM role_parties");
        jdbc.update("DELETE FROM guild_members WHERE guild_id = ?", GUILD);
        jdbc.update("DELETE FROM guild_members WHERE guild_id = 63001");
        jdbc.update("DELETE FROM party_members WHERE party_id = ?", PARTY);
        jdbc.update("DELETE FROM guilds WHERE guild_id = ?", GUILD);
        jdbc.update("DELETE FROM guilds WHERE guild_id = 63001");
        jdbc.update("DELETE FROM parties WHERE party_id BETWEEN 63001 AND 63010");
        jdbc.update("DELETE FROM parties WHERE party_id = ?", PARTY);
        jdbc.update("DELETE FROM roles WHERE player_id = ?", A);
        jdbc.update("DELETE FROM player WHERE id BETWEEN ? AND ?", A, D);
        given(userAuthApi.resolveAuthorization(any())).willReturn(Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        for (long id : List.of(A, B, C, D)) jdbc.update("""
                INSERT INTO player (id, user_id, name, level, exp, hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat, extra_stats,
                    status_effects, version, created_at, updated_at)
                VALUES (?, ?, 'fixture', 1, 0, 100, 100, 50, 50, 1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0, NOW(6), NOW(6))
                """, id, id);
        jdbc.update("""
                INSERT INTO guilds (guild_id, player_id, leader_player_id, name_original, name_value,
                    code_value, visibility, join_policy, status, max_members, created_at, updated_at)
                VALUES (?, ?, ?, 'guild', 'guild', 'GGE-GUILD', 'PRIVATE', 'APPROVAL', 'ACTIVE', 10, NOW(6), NOW(6))
                """, GUILD, A, A);
        for (long id : List.of(A, C)) jdbc.update("""
                INSERT INTO guild_members (guild_id, player_id, role, joined_at, created_at, updated_at)
                VALUES (?, ?, ?, NOW(6), NOW(6), NOW(6))
                """, GUILD, id, id == A ? "LEADER" : "MEMBER");
        jdbc.update("""
                INSERT INTO parties (party_id, player_id, leader_player_id, name_original, name_value,
                    code_value, visibility, join_policy, status, max_members, created_at, updated_at)
                VALUES (?, ?, ?, 'secret source name', 'secret source name', 'GGE-PARTY', 'PRIVATE', 'INVITE_ONLY', 'ACTIVE', 10, NOW(6), NOW(6))
                """, PARTY, B, B);
        jdbc.update("""
                INSERT INTO party_members (party_id, player_id, role, joined_at, created_at, updated_at)
                VALUES (?, ?, 'LEADER', NOW(6), NOW(6), NOW(6))
                """, PARTY, B);
        jdbc.update("""
                INSERT INTO roles (player_id, role_type, name, status, created_at, updated_at, version)
                VALUES (?, 'WORK', 'private role', 'ACTIVE', NOW(6), NOW(6), 0)
                """, A);
    }

    @Test
    @DisplayName("두 리더 승인 후 안전한 목록을 보이고 해제해도 원본 모임은 유지한다")
    void linksWithBothLeaders() throws Exception {
        String base = "/api/v1/guilds/" + GUILD + "/group-links";
        long link = result(mvc.perform(auth(post(base), A).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupType\":\"PARTY\",\"groupId\":" + PARTY + ",\"displayName\":\"공유 모임\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(auth(get(base), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(0));
        mvc.perform(auth(get(base + "/pending"), B)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1));
        mvc.perform(auth(get(base + "/pending"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(0));
        mvc.perform(auth(post(base + "/" + link + "/approve"), B).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"공개 별칭\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOC-409-GUILD-GROUP-CONFLICT"));
        mvc.perform(auth(get(base + "/pending"), B)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].displayName").value("공유 모임"))
                .andExpect(jsonPath("$.result.contents[0].groupLeaderApproved").value(false));
        mvc.perform(auth(post(base + "/" + link + "/approve"), B).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\" 공유 모임 \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("ACTIVE"));
        String list = mvc.perform(auth(get(base), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].entryAction").value("INVITE_REQUIRED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains("공유 모임").doesNotContain("secret source name", "private role", "proposedByPlayerId");
        mvc.perform(auth(get("/api/v1/parties/" + PARTY), C)).andExpect(status().isNotFound());
        mvc.perform(auth(delete(base + "/" + link), B)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parties WHERE party_id = ?", Long.class, PARTY)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM party_members WHERE party_id = ?", Long.class, PARTY)).isEqualTo(1);
        mvc.perform(auth(get(base), C)).andExpect(status().isOk()).andExpect(jsonPath("$.result.totalElements").value(0));
        long next = result(mvc.perform(auth(post(base), B).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupType\":\"PARTY\",\"groupId\":" + PARTY + ",\"displayName\":\"다시 연결\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        assertThat(next).isNotEqualTo(link);
        mvc.perform(auth(post(base + "/" + next + "/cancel"), B)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_group_links WHERE guild_id = ?", Long.class, GUILD)).isEqualTo(2);
        long pending = result(mvc.perform(auth(post(base), A).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupType\":\"PARTY\",\"groupId\":" + PARTY + ",\"displayName\":\"새 리더 확인\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(auth(post("/api/v1/guilds/" + GUILD + "/transfer-leader"), A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"toPlayerId\":" + C + "}"))
                .andExpect(status().isOk());
        mvc.perform(auth(post(base + "/" + pending + "/approve"), B).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"새 리더 확인\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("PENDING"))
                .andExpect(jsonPath("$.result.guildLeaderApproved").value(false));
        mvc.perform(auth(post(base + "/" + pending + "/approve"), C).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"새 리더 확인\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("행사는 현재 길드원만 읽고 RSVP하며 종료·탈퇴 후 참가 이력을 보존한다")
    void eventOwnershipAndHistory() throws Exception {
        String base = "/api/v1/guilds/" + GUILD + "/events";
        String body = "{\"title\":\"공유 행사\",\"sharedDescription\":\"모임\",\"startsAt\":\"2026-11-01T10:00:00Z\",\"endsAt\":\"2026-11-01T11:00:00Z\",\"location\":null}";
        long lifelogs = jdbc.queryForObject("SELECT COUNT(*) FROM life_log_records", Long.class);
        long outbox = jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class);
        long eventId = result(mvc.perform(auth(post(base), A).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        String path = base + "/" + eventId;
        mvc.perform(auth(get(path), D)).andExpect(status().isNotFound());
        mvc.perform(auth(put(path + "/rsvp"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.participantCount").value(1));
        mvc.perform(auth(put(path + "/rsvp"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.participantCount").value(1));
        mvc.perform(auth(get(path), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.myRsvp").value(true));
        mvc.perform(auth(post("/api/v1/guilds/" + GUILD + "/transfer-leader"), A)
                .contentType(MediaType.APPLICATION_JSON).content("{\"toPlayerId\":" + C + "}"))
                .andExpect(status().isOk());
        mvc.perform(auth(post(path + "/complete"), A)).andExpect(status().isForbidden());
        mvc.perform(auth(post(path + "/complete"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("COMPLETED"));
        mvc.perform(auth(post("/api/v1/guilds/" + GUILD + "/transfer-leader"), C)
                .contentType(MediaType.APPLICATION_JSON).content("{\"toPlayerId\":" + A + "}"))
                .andExpect(status().isOk());
        mvc.perform(auth(post(path + "/complete"), C)).andExpect(status().isForbidden());
        mvc.perform(auth(post(path + "/complete"), A)).andExpect(status().isConflict());
        mvc.perform(auth(delete(path + "/rsvp"), C)).andExpect(status().isConflict());
        jdbc.update("DELETE FROM guild_members WHERE guild_id = ? AND player_id = ?", GUILD, C);
        mvc.perform(auth(get(path), C)).andExpect(status().isNotFound());
        mvc.perform(auth(get(path), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.participantCount").value(0));
        mvc.perform(auth(post(base), A).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"역전\",\"startsAt\":\"2026-11-01T11:00:00Z\",\"endsAt\":\"2026-11-01T10:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        long cancelled = result(mvc.perform(auth(post(base), A).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(auth(post(base + "/" + cancelled + "/cancel"), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CANCELED"));
        mvc.perform(auth(patch(base + "/" + cancelled), A).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_event_rsvps WHERE guild_event_id = ?", Long.class, eventId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM life_log_records", Long.class)).isEqualTo(lifelogs);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class)).isEqualTo(outbox);
    }

    @Test
    @DisplayName("한 사람이 두 리더이면 RoleParty를 즉시 연결하고 개인 Role은 노출하지 않는다")
    void linksOwnedRolePartyWithoutRoleDisclosure() throws Exception {
        long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE player_id = ?", Long.class, A);
        long rolePartyId = result(mvc.perform(auth(post("/api/v1/roles/" + roleId + "/role-parties"), A)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"private role party\",\"description\":\"secret\",\"maxMembers\":3}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        String base = "/api/v1/guilds/" + GUILD + "/group-links";
        mvc.perform(auth(post(base), A).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupType\":\"ROLE_PARTY\",\"groupId\":" + rolePartyId + ",\"displayName\":\"탐색 이름\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.result.status").value("ACTIVE"));
        String list = mvc.perform(auth(get(base), C)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains("탐색 이름", "INVITE_REQUIRED")
                .doesNotContain("private role party", "private role", "secret", "roleId");
        mvc.perform(auth(get("/api/v1/role-parties/" + rolePartyId), C)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("대기 목록은 Party·RoleParty의 현재 리더와 제안자에게 보이는 행만 DB에서 페이지로 센다")
    void pendingPagesOnlyVisibleRows() throws Exception {
        String base = "/api/v1/guilds/" + GUILD + "/group-links/pending";
        for (long id = 63001; id <= 63003; id++) jdbc.update("""
                INSERT INTO parties (party_id, player_id, leader_player_id, name_original, name_value,
                    code_value, visibility, join_policy, status, max_members, created_at, updated_at)
                VALUES (?, ?, ?, 'source', 'source', ?, 'PRIVATE', 'INVITE_ONLY', ?, 10, NOW(6), NOW(6))
                """, id, id == 63001 ? C : B, id == 63001 ? C : B, "GGE-" + id,
                id == 63003 ? "DISBANDED" : "ACTIVE");
        long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE player_id = ?", Long.class, A);
        jdbc.update("""
                INSERT INTO role_parties (id, role_id, creator_player_id, leader_player_id, name,
                    status, max_members, version, created_at, updated_at)
                VALUES (63001, ?, ?, ?, 'private source', 'ACTIVE', 3, 0, NOW(6), NOW(6))
                """, roleId, A, C);
        for (long id = 63101; id <= 63105; id++) {
            String type = id == 63104 ? "ROLE_PARTY" : "PARTY";
            long group = switch ((int) id) {
                case 63101 -> PARTY;       // hidden: B leads, A proposed
                case 63102 -> 63001;       // visible: C leads
                case 63103 -> 63002;       // visible: C proposed
                case 63104 -> 63001;       // visible: C leads RoleParty
                default -> 63003;         // hidden: disbanded Party leader
            };
            jdbc.update("""
                    INSERT INTO guild_group_links (id, guild_id, group_type, group_id, display_name,
                        status, proposed_by_player_id, open_slot, version, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'safe label', 'PENDING', ?, 1, 0, NOW(6), NOW(6))
                    """, id, GUILD, type, group, id == 63103 ? C : A);
        }
        jdbc.update("""
                INSERT INTO guilds (guild_id, player_id, leader_player_id, name_original, name_value,
                    code_value, visibility, join_policy, status, max_members, created_at, updated_at)
                VALUES (63001, ?, ?, 'other guild', 'other guild', 'GGE-OTHER',
                    'PRIVATE', 'APPROVAL', 'ACTIVE', 10, NOW(6), NOW(6))
                """, D, D);
        jdbc.update("""
                INSERT INTO guild_group_links (guild_id, group_type, group_id, display_name,
                    status, proposed_by_player_id, open_slot, version, created_at, updated_at)
                VALUES (63001, 'PARTY', ?, 'other label', 'PENDING', ?, 1, 0, NOW(6), NOW(6))
                """, PARTY, D);
        mvc.perform(auth(get(base + "?page=0&size=2"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.contents[0].id").value(63104))
                .andExpect(jsonPath("$.result.contents[1].id").value(63103));
        mvc.perform(auth(get(base + "?page=1&size=2"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].id").value(63102))
                .andExpect(jsonPath("$.result.contents.length()").value(1));
        mvc.perform(auth(get(base + "?page=2&size=2"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.contents.length()").value(0));
        mvc.perform(auth(get(base + "?page=-1&size=2"), C)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("행사 목록과 참가 페이지는 현재 길드원만 집계하며 동시각 RSVP도 ID 순서를 유지한다")
    void eventPagesExcludeFormerMembers() throws Exception {
        String base = "/api/v1/guilds/" + GUILD + "/events";
        for (long id = 63101; id <= 63103; id++) jdbc.update("""
                INSERT INTO guild_events (id, guild_id, title, starts_at, ends_at, status,
                    created_by_player_id, version, created_at, updated_at)
                VALUES (?, ?, 'page event', '2026-11-01 10:00:00', '2026-11-01 11:00:00',
                    'PLANNED', ?, 0, NOW(6), NOW(6))
                """, id, GUILD, A);
        for (long player : List.of(A, C, D)) jdbc.update("""
                INSERT INTO guild_event_rsvps (guild_event_id, player_id, joined_at, active, created_at, updated_at)
                VALUES (63103, ?, '2026-11-01 09:00:00', 1, NOW(6), NOW(6))
                """, player);
        mvc.perform(auth(get(base + "?page=0&size=2"), C)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.contents[0].id").value(63103))
                .andExpect(jsonPath("$.result.contents[0].participantCount").value(2))
                .andExpect(jsonPath("$.result.contents[0].myRsvp").value(true));
        mvc.perform(auth(get(base + "?page=1&size=2"), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].id").value(63101));
        String participants = base + "/63103/participants";
        mvc.perform(auth(get(participants + "?page=0&size=1"), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.contents[0].playerId").value(A));
        mvc.perform(auth(get(participants + "?page=1&size=1"), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].playerId").value(C));
        jdbc.update("DELETE FROM guild_members WHERE guild_id = ? AND player_id = ?", GUILD, C);
        mvc.perform(auth(get(participants), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1));
        mvc.perform(auth(get(base), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].participantCount").value(1));
        mvc.perform(auth(get(participants + "?page=0&size=0"), A)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("승인과 리더 이전 경쟁은 직렬화되고 이전 리더의 후속 승인은 거부한다")
    void approvalAndLeadershipTransferSerialize() throws Exception {
        String base = "/api/v1/guilds/" + GUILD + "/group-links";
        long link = result(mvc.perform(auth(post(base), A).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupType\":\"PARTY\",\"groupId\":" + PARTY + ",\"displayName\":\"동일 이름\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var approve = pool.submit(() -> { start.await(); return mvc.perform(auth(post(base + "/" + link + "/approve"), B)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"동일 이름\"}"))
                    .andReturn().getResponse().getStatus(); });
            var transfer = pool.submit(() -> { start.await(); return mvc.perform(auth(post("/api/v1/guilds/" + GUILD + "/transfer-leader"), A)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"toPlayerId\":" + C + "}"))
                    .andReturn().getResponse().getStatus(); });
            start.countDown();
            assertThat(approve.get(15, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(transfer.get(15, TimeUnit.SECONDS)).isEqualTo(200);
        }
        String state = jdbc.queryForObject("SELECT status FROM guild_group_links WHERE id = ?", String.class, link);
        assertThat(state).isIn("PENDING", "ACTIVE");
        mvc.perform(auth(post(base + "/" + link + "/approve"), A).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"동일 이름\"}"))
                .andExpect(status().isNotFound());
        if (state.equals("PENDING")) {
            assertThat(jdbc.queryForObject("SELECT guild_approved_by_player_id FROM guild_group_links WHERE id = ?", Long.class, link)).isNull();
            mvc.perform(auth(post(base + "/" + link + "/approve"), C).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"displayName\":\"동일 이름\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("ACTIVE"));
        }
    }

    @Test
    @DisplayName("동시 제안과 종료·참가 경쟁은 MySQL에서 중복 연결과 종료 후 참가를 막는다")
    void serializesCompetingCommands() throws Exception {
        String links = "/api/v1/guilds/" + GUILD + "/group-links";
        String proposal = "{\"groupType\":\"PARTY\",\"groupId\":" + PARTY + ",\"displayName\":\"공유 별칭\"}";
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { start.await(); return mvc.perform(auth(post(links), A)
                    .contentType(MediaType.APPLICATION_JSON).content(proposal)).andReturn().getResponse().getStatus(); });
            var b = pool.submit(() -> { start.await(); return mvc.perform(auth(post(links), B)
                    .contentType(MediaType.APPLICATION_JSON).content(proposal)).andReturn().getResponse().getStatus(); });
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 201);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_group_links WHERE guild_id = ? AND open_slot = 1", Long.class, GUILD)).isEqualTo(1);

        String events = "/api/v1/guilds/" + GUILD + "/events";
        String body = "{\"title\":\"동시 행사\",\"startsAt\":\"2026-11-01T10:00:00Z\",\"endsAt\":\"2026-11-01T11:00:00Z\"}";
        long id = result(mvc.perform(auth(post(events), A).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
        String path = events + "/" + id;
        CountDownLatch race = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var complete = pool.submit(() -> { race.await(); return mvc.perform(auth(post(path + "/complete"), A)).andReturn().getResponse().getStatus(); });
            var rsvp = pool.submit(() -> { race.await(); return mvc.perform(auth(put(path + "/rsvp"), C)).andReturn().getResponse().getStatus(); });
            race.countDown();
            assertThat(complete.get(15, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(rsvp.get(15, TimeUnit.SECONDS)).isIn(200, 409);
        }
        mvc.perform(auth(get(path), A)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("COMPLETED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_event_rsvps WHERE guild_event_id = ?", Long.class, id)).isBetween(0L, 1L);
    }

    private JsonNode result(String body) throws Exception { return json.readTree(body).path("result"); }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, long playerId) {
        return request.header("Authorization", "Bearer " + jwt.createAccessToken(playerId, playerId));
    }

    @TestConfiguration
    static class IdentityConfig {
        @Bean @Primary CurrentPlayerAccessor guildGroupsEventsPlayerAccessor() { return new JwtCurrentPlayerAccessor(); }
    }
}
