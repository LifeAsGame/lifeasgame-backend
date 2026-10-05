package online.lifeasgame.role.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(RoleScheduleIntegrationTest.IdentityConfig.class)
@DisplayName("Role 개인 일정과 직접 연결 Guild 일정의 HTTP·MySQL 읽기 계약")
class RoleScheduleIntegrationTest {
    private static final long A = 87001, B = 87002, C = 87003;
    private static final long ROLE = 87101, OTHER_ROLE = 87102, FOREIGN_ROLE = 87103;
    private static final long GUILD = 87201, OTHER_GUILD = 87202;
    private static final String FROM = "2026-10-31T14:30:00Z";
    private static final String TO = "2026-10-31T16:00:00Z";

    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("role_schedule").withUsername("lifeasgame").withPassword("lifeasgame");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtProvider jwt;
    @MockitoBean UserAuthApi userAuthApi;

    @BeforeEach void seed() {
        for (String table : List.of("group_activity_rsvps", "group_activity_editors", "group_activities",
                "role_party_invitations", "role_party_members", "role_parties", "party_wait_members", "party_members", "parties",
                "guild_event_rsvps", "guild_events", "personal_role_group_links",
                "guild_members", "guild_wait_members", "guilds", "role_events", "roles", "player"))
            jdbc.update("DELETE FROM " + table);
        given(userAuthApi.resolveAuthorization(any())).willReturn(Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        for (long id : List.of(A, B, C)) jdbc.update("""
                INSERT INTO player (id,user_id,name,level,exp,hp_cur,hp_cap,mp_cur,mp_cap,
                 str_stat,agi_stat,dex_stat,int_stat,vit_stat,luc_stat,extra_stats,status_effects,version,created_at,updated_at)
                VALUES (?,?,'fixture',1,0,100,100,50,50,1,1,1,1,1,1,JSON_OBJECT(),'[]',0,NOW(6),NOW(6))
                """, id, id);
        for (long id : List.of(ROLE, OTHER_ROLE, FOREIGN_ROLE)) jdbc.update("""
                INSERT INTO roles (id,player_id,role_type,name,status,version,created_at,updated_at)
                VALUES (?,?,'WORK','fixture','ACTIVE',0,NOW(6),NOW(6))
                """, id, id == FOREIGN_ROLE ? C : A);
        for (long id : List.of(GUILD, OTHER_GUILD)) {
            jdbc.update("""
                    INSERT INTO guilds (guild_id,player_id,leader_player_id,name_original,name_value,
                      code_value,visibility,join_policy,status,max_members,created_at,updated_at)
                    VALUES (?,?,?,'Private Guild','private guild',?,'PRIVATE','INVITE_ONLY','ACTIVE',10,NOW(6),NOW(6))
                    """, id, B, B, "SCHEDULE-" + id);
            for (long member : List.of(A, B)) jdbc.update("""
                    INSERT INTO guild_members(guild_id,player_id,role,joined_at,created_at,updated_at)
                    VALUES (?,?,?,NOW(6),NOW(6),NOW(6))
                    """, id, member, member == B ? "LEADER" : "MEMBER");
        }
    }

    @Nested @DisplayName("Party와 RoleParty 공유 활동을 연결하면")
    class GroupActivities {
        @Test @DisplayName("리더가 지정한 편집자만 쓰고, 생성 재시도·버전·RSVP를 보존한다")
        void permissionsAndCommands() throws Exception {
            groups();
            String party = "/api/v1/parties/87301/activities";
            String roleParty = "/api/v1/role-parties/87302/activities";
            for (String base : List.of(party, roleParty)) {
                String key = UUID.randomUUID().toString();
                result(post(base).content(create(key, "first")), A, 403);
                result(get(base), C, 404);
                result(put(base + "/editors/" + A), A, 403);
                result(put(base + "/editors/" + A), B, 204);
                var created = result(post(base).content(create(key, "first")), A, 201);
                long id = created.path("id").asLong();
                assertThat(result(post(base).content(create(key, "first")), A, 201).path("id").asLong()).isEqualTo(id);
                result(post(base).content(create(key, "different")), A, 409);
                assertThat(result(get(base), B, 200).path("totalElements").asInt()).isEqualTo(1);
                result(get(base + "/" + id), C, 404);
                assertThat(result(put(base + "/" + id + "/rsvp"), A, 200).path("participantCount").asInt()).isEqualTo(1);
                assertThat(result(put(base + "/" + id + "/rsvp"), A, 200).path("participantCount").asInt()).isEqualTo(1);
                assertThat(result(get(base + "/" + id + "/participants"), B, 200).path("totalElements").asInt()).isEqualTo(1);
                result(patch(base + "/" + id).content(edit(7, "stale")), A, 409);
                assertThat(result(patch(base + "/" + id).content(edit(0, "updated")), A, 200).path("version").asLong()).isEqualTo(1);
                result(delete(base + "/editors/" + A), B, 204);
                result(patch(base + "/" + id).content(edit(1, "blocked")), A, 403);
                assertThat(result(post(base + "/" + id + "/complete").content("{\"version\":1}"), B, 200)
                        .path("status").asText()).isEqualTo("COMPLETED");
                result(put(base + "/" + id + "/rsvp"), A, 409);
                result(post(base + "/" + id + "/cancel").content("{\"version\":2}"), B, 409);
            }
        }

        @Test @DisplayName("네 소스를 한 페이지로 정렬하고 연결·멤버십 상실을 즉시 숨긴다")
        void mixedScheduleAndRevocation() throws Exception {
            groups();
            roleEvent(88001, ROLE, "personal", "2026-10-31 15:00:00", "2026-10-31 15:30:00", "PLANNED");
            guildEvent(88001, GUILD, "guild", "2026-10-31 15:00:00", "2026-10-31 15:30:00", "PLANNED");
            link(ROLE, GUILD);
            jdbc.update("INSERT INTO personal_role_group_links(owner_player_id,role_id,group_type,group_id) VALUES (?,?,?,?)", A, ROLE, "PARTY", 87301);
            jdbc.update("INSERT INTO personal_role_group_links(owner_player_id,role_id,group_type,group_id) VALUES (?,?,?,?)", A, ROLE, "ROLE_PARTY", 87302);
            var p = result(post("/api/v1/parties/87301/activities").content(create(UUID.randomUUID().toString(), "party")), B, 201);
            var rp = result(post("/api/v1/role-parties/87302/activities").content(create(UUID.randomUUID().toString(), "role party")), B, 201);
            result(put("/api/v1/parties/87301/activities/" + p.path("id").asLong() + "/rsvp"), A, 200);
            var all = result(window(ROLE).param("size", "2"), A, 200);
            assertThat(all.path("totalElements").asInt()).isEqualTo(4);
            assertThat(all.at("/contents/0/sourceType").asText()).isEqualTo("GUILD_EVENT");
            assertThat(all.at("/contents/1/sourceType").asText()).isEqualTo("PARTY_ACTIVITY");
            var second = result(window(ROLE).param("size", "2").param("page", "1"), A, 200);
            assertThat(second.at("/contents/0/sourceType").asText()).isEqualTo("ROLE_EVENT");
            assertThat(second.at("/contents/1/sourceType").asText()).isEqualTo("ROLE_PARTY_ACTIVITY");
            assertThat(result(window(ROLE).param("source", "SHARED").param("participating", "true"), A, 200)
                    .path("totalElements").asInt()).isEqualTo(1);
            assertThat(result(window(ROLE).param("source", "ROLE_PARTY"), A, 200)
                    .at("/contents/0/groupId").asLong()).isEqualTo(87302);
            jdbc.update("UPDATE role_party_members SET left_at=NOW(6) WHERE role_party_id=87302 AND player_id=?", A);
            assertThat(result(window(ROLE), A, 200).path("totalElements").asInt()).isEqualTo(3);
            result(get("/api/v1/role-parties/87302/activities/" + rp.path("id").asLong()), A, 404);
            jdbc.update("DELETE FROM personal_role_group_links WHERE role_id=? AND group_type='PARTY'", ROLE);
            assertThat(result(window(ROLE), A, 200).path("totalElements").asInt()).isEqualTo(2);
            assertThat(result(get("/api/v1/parties/87301/activities/" + p.path("id").asLong()), A, 200)
                    .path("myRsvp").asBoolean()).isTrue();
        }

        @Test @DisplayName("리더 이전과 재가입 후 옛 편집 권한·RSVP가 복원되지 않는다")
        void transferAndRejoin() throws Exception {
            groups();
            String party = "/api/v1/parties/87301/activities";
            String roleParty = "/api/v1/role-parties/87302/activities";
            result(put(roleParty + "/editors/" + A), B, 204);
            long activityId = result(post(roleParty).content(create(UUID.randomUUID().toString(), "meeting")), A, 201)
                    .path("id").asLong();
            result(put(roleParty + "/" + activityId + "/rsvp"), A, 200);
            result(post("/api/v1/role-parties/87302/leave"), A, 204);
            result(get(roleParty + "/" + activityId), A, 404);
            jdbc.update("UPDATE role_party_members SET left_at=NULL, joined_at=DATE_ADD(joined_at, INTERVAL 1 SECOND)"
                    + " WHERE role_party_id=87302 AND player_id=?", A);
            var rejoined = result(get(roleParty + "/" + activityId), A, 200);
            assertThat(rejoined.path("myRsvp").asBoolean()).isFalse();
            assertThat(rejoined.path("participantCount").asInt()).isZero();
            assertThat(rejoined.at("/capabilities/canEdit").asBoolean()).isFalse();
            result(post(roleParty + "/" + activityId + "/complete").content("{\"version\":0}"), A, 403);
            result(post("/api/v1/role-parties/87302/transfer-leader").content("{\"toPlayerId\":" + A + "}"), B, 200);
            result(post(roleParty).content(create(UUID.randomUUID().toString(), "old leader")), B, 403);
            result(post(roleParty).content(create(UUID.randomUUID().toString(), "new leader")), A, 201);
            result(post("/api/v1/parties/87301/transfer-leader")
                    .content("{\"fromLeaderPlayerId\":" + B + ",\"toPlayerId\":" + A + "}"), B, 200);
            result(post(party).content(create(UUID.randomUUID().toString(), "old party leader")), B, 403);
            result(post(party).content(create(UUID.randomUUID().toString(), "new party leader")), A, 201);
        }

        @Test @DisplayName("동시 수정은 한 요청만 반영하고, 권한 회수 후 쓰기를 거부한다")
        void serializesMutations() throws Exception {
            groups();
            String base = "/api/v1/parties/87301/activities";
            result(put(base + "/editors/" + A), B, 204);
            long id = result(post(base).content(create(UUID.randomUUID().toString(), "before")), B, 201)
                    .path("id").asLong();
            try (var pool = Executors.newFixedThreadPool(2)) {
                var start = new CountDownLatch(1);
                var first = pool.submit(() -> {
                    start.await();
                    return statusOf(patch(base + "/" + id).content(edit(0, "first")), A);
                });
                var second = pool.submit(() -> {
                    start.await();
                    return statusOf(patch(base + "/" + id).content(edit(0, "second")), B);
                });
                start.countDown();
                assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder(200, 409);
            }
            assertThat(result(get(base + "/" + id), A, 200).path("version").asLong()).isEqualTo(1);
            try (var pool = Executors.newFixedThreadPool(2)) {
                var start = new CountDownLatch(1);
                var edit = pool.submit(() -> {
                    start.await();
                    return statusOf(patch(base + "/" + id).content(edit(1, "racing edit")), A);
                });
                var revoke = pool.submit(() -> {
                    start.await();
                    return statusOf(delete(base + "/editors/" + A), B);
                });
                start.countDown();
                assertThat(edit.get(15, TimeUnit.SECONDS)).isIn(200, 403);
                assertThat(revoke.get(15, TimeUnit.SECONDS)).isEqualTo(204);
            }
            long latestVersion = result(get(base + "/" + id), B, 200).path("version").asLong();
            assertThat(latestVersion).isIn(1L, 2L);
            result(patch(base + "/" + id).content(edit(1, "blocked")), A, 403);
            String terminal = base + "/" + id + "/complete";
            result(post(terminal).content("{\"version\":" + latestVersion + "}"), B, 200);
            result(put(base + "/" + id + "/rsvp"), A, 409);

            String requestKey = UUID.randomUUID().toString();
            try (var pool = Executors.newFixedThreadPool(2)) {
                var start = new CountDownLatch(1);
                var first = pool.submit(() -> {
                    start.await();
                    return result(post(base).content(create(requestKey, "same request")), B, 201).path("id").asLong();
                });
                var second = pool.submit(() -> {
                    start.await();
                    return result(post(base).content(create(requestKey, "same request")), B, 201).path("id").asLong();
                });
                start.countDown();
                assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
            }
            assertThat(result(get(base), B, 200).path("totalElements").asInt()).isEqualTo(2);
            long nextId = result(post(base).content(create(UUID.randomUUID().toString(), "finish race")), B, 201)
                    .path("id").asLong();
            try (var pool = Executors.newFixedThreadPool(2)) {
                var start = new CountDownLatch(1);
                var complete = pool.submit(() -> {
                    start.await();
                    return statusOf(post(base + "/" + nextId + "/complete").content("{\"version\":0}"), B);
                });
                var rsvp = pool.submit(() -> {
                    start.await();
                    return statusOf(put(base + "/" + nextId + "/rsvp"), A);
                });
                start.countDown();
                assertThat(complete.get(15, TimeUnit.SECONDS)).isEqualTo(200);
                assertThat(rsvp.get(15, TimeUnit.SECONDS)).isIn(200, 409);
            }
            assertThat(result(get(base + "/" + nextId), A, 200).path("status").asText()).isEqualTo("COMPLETED");
        }
    }

    private void groups() {
        jdbc.update("""
                INSERT INTO parties (party_id,player_id,leader_player_id,name_original,name_value,code_value,
                 visibility,join_policy,status,max_members,created_at,updated_at)
                VALUES (87301,?,?, 'Party','party','SCHEDULE-PARTY','PUBLIC','APPROVAL','ACTIVE',10,NOW(6),NOW(6))
                """, B, B);
        for (long id : List.of(A, B)) jdbc.update("""
                INSERT INTO party_members(party_id,player_id,role,joined_at,created_at,updated_at)
                VALUES (87301,?,?,NOW(6),NOW(6),NOW(6))
                """, id, id == B ? "LEADER" : "OFFICER");
        jdbc.update("""
                INSERT INTO role_parties(id,role_id,creator_player_id,leader_player_id,name,status,max_members,version,created_at,updated_at)
                VALUES (87302,?,?,?,'Role Party','ACTIVE',10,0,NOW(6),NOW(6))
                """, FOREIGN_ROLE, C, B);
        for (long id : List.of(A, B)) jdbc.update("""
                INSERT INTO role_party_members(role_party_id,player_id,joined_at,created_at,updated_at)
                VALUES (87302,?,NOW(6),NOW(6),NOW(6))
                """, id);
    }

    private static String create(String key, String title) {
        return "{\"clientRequestId\":\"" + key + "\",\"title\":\"" + title
                + "\",\"startsAt\":\"2026-10-31T15:00:00Z\",\"endsAt\":\"2026-10-31T15:30:00Z\"}";
    }
    private static String edit(long version, String title) {
        return "{\"version\":" + version + ",\"title\":\"" + title
                + "\",\"startsAt\":\"2026-10-31T15:00:00Z\",\"endsAt\":\"2026-10-31T15:30:00Z\"}";
    }

    @Nested @DisplayName("혼합 목록을 조회하면")
    class Combined {
        @Test @DisplayName("현재 직접 연결만 전역 정렬·페이지하고 동일 숫자 원본 ID를 구분한다")
        void globallyPaginates() throws Exception {
            roleEvent(88001, ROLE, "personal", "2026-10-31 14:45:00", "2026-10-31 15:15:00", "PLANNED");
            roleEvent(88002, ROLE, "point", "2026-10-31 15:00:00", "2026-10-31 15:00:00", "PLANNED");
            roleEvent(88003, OTHER_ROLE, "other role", "2026-10-31 15:00:00", null, "PLANNED");
            guildEvent(88001, GUILD, "shared", "2026-10-31 15:00:00", "2026-10-31 16:00:00", "PLANNED");
            guildEvent(88002, OTHER_GUILD, "unlinked", "2026-10-31 15:30:00", "2026-10-31 16:00:00", "PLANNED");
            long link = link(ROLE, GUILD);
            link(OTHER_ROLE, GUILD);
            var first = result(window(ROLE).param("size", "1"), A, 200);
            assertThat(first.path("totalElements").asInt()).isEqualTo(3);
            assertThat(first.at("/contents/0/sourceType").asText()).isEqualTo("ROLE_EVENT");
            assertThat(first.at("/contents/0/sourceId").asLong()).isEqualTo(88001);
            assertThat(first.at("/contents/0/myRsvp").isNull()).isTrue();
            var second = result(window(ROLE).param("size", "1").param("page", "1"), A, 200);
            assertThat(second.at("/contents/0/sourceType").asText()).isEqualTo("GUILD_EVENT");
            assertThat(second.at("/contents/0/sourceId").asLong()).isEqualTo(88001);
            assertThat(second.at("/contents/0/myRsvp").asBoolean()).isFalse();
            assertThat(second.at("/contents/0/guildName").asText()).isEqualTo("Private Guild");
            assertThat(result(window(ROLE).param("size", "1").param("page", "2"), A, 200)
                    .at("/contents/0/sourceId").asLong()).isEqualTo(88002);
            assertThat(result(window(ROLE).param("source", "GUILD"), A, 200).path("totalElements").asInt()).isEqualTo(1);
            assertThat(result(window(OTHER_ROLE), A, 200).path("totalElements").asInt()).isEqualTo(2);
            assertThat(result(get("/api/v1/roles/" + ROLE + "/events"), A, 200).size()).isEqualTo(2);
            var facts = counts();
            result(window(ROLE), A, 200);
            assertThat(counts()).isEqualTo(facts);
            result(delete("/api/v1/roles/" + ROLE + "/group-links/" + link), A, 204);
            assertThat(result(window(ROLE), A, 200).path("totalElements").asInt()).isEqualTo(2);
            assertThat(result(window(OTHER_ROLE), A, 200).path("totalElements").asInt()).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_events WHERE id = 88001", Long.class)).isEqualTo(1);
        }

        @Test @DisplayName("RSVP·원본 변경·완료 상태를 다음 조회에 반영하고 참가 필터는 Guild에만 적용한다")
        void reflectsSourceChanges() throws Exception {
            roleEvent(88001, ROLE, "personal", "2026-10-31 15:00:00", null, "PLANNED");
            guildEvent(88001, GUILD, "before", "2026-10-31 15:00:00", "2026-10-31 16:00:00", "PLANNED");
            link(ROLE, GUILD);
            var attended = window(ROLE).param("source", "GUILD").param("participating", "true");
            assertThat(result(attended, A, 200).path("totalElements").asInt()).isZero();
            result(put("/api/v1/guilds/" + GUILD + "/events/88001/rsvp"), A, 200);
            assertThat(result(window(ROLE).param("source", "GUILD").param("participating", "true"), A, 200)
                    .at("/contents/0/myRsvp").asBoolean()).isTrue();
            result(delete("/api/v1/guilds/" + GUILD + "/events/88001/rsvp"), A, 204);
            assertThat(result(window(ROLE).param("source", "GUILD").param("participating", "true"), A, 200)
                    .path("totalElements").asInt()).isZero();
            result(put("/api/v1/guilds/" + GUILD + "/events/88001/rsvp"), A, 200);
            result(patch("/api/v1/guilds/" + GUILD + "/events/88001").content("""
                    {"title":"after","startsAt":"2026-10-31T15:30:00Z","endsAt":"2026-10-31T16:00:00Z"}
                    """), B, 200);
            var changed = result(window(ROLE).param("source", "GUILD"), A, 200);
            assertThat(changed.at("/contents/0/title").asText()).isEqualTo("after");
            assertThat(changed.at("/contents/0/startsAt").asText()).isEqualTo("2026-10-31T15:30:00Z");
            result(post("/api/v1/guilds/" + GUILD + "/events/88001/complete"), B, 200);
            assertThat(result(window(ROLE).param("source", "GUILD"), A, 200).path("totalElements").asInt()).isZero();
            var history = result(window(ROLE).param("source", "GUILD").param("status", "COMPLETED"), A, 200);
            assertThat(history.at("/contents/0/title").asText()).isEqualTo("after");
            assertThat(history.at("/contents/0/status").asText()).isEqualTo("COMPLETED");
            assertThat(result(window(ROLE).param("source", "GUILD").param("status", "ALL")
                    .param("participating", "true"), A, 200).path("totalElements").asInt()).isEqualTo(1);
            guildEvent(88002, GUILD, "cancelled", "2026-10-31 15:10:00", "2026-10-31 15:20:00", "PLANNED");
            result(post("/api/v1/guilds/" + GUILD + "/events/88002/cancel"), B, 200);
            assertThat(result(window(ROLE).param("status", "CANCELED"), A, 200)
                    .at("/contents/0/sourceId").asLong()).isEqualTo(88002);
        }
    }

    @Nested @DisplayName("기간과 현재 권한을 적용하면")
    class AccessAndTime {
        @Test @DisplayName("탈퇴·해산·타인 Role을 숨기고 보관 Role 개인 이력은 유지한다")
        void hidesRevokedGuild() throws Exception {
            roleEvent(88001, ROLE, "history", "2026-10-31 15:00:00", null, "PLANNED");
            guildEvent(88001, GUILD, "secret", "2026-10-31 15:00:00", "2026-10-31 16:00:00", "PLANNED");
            link(ROLE, GUILD);
            result(window(ROLE), C, 404);
            result(window(FOREIGN_ROLE), A, 404);
            assertThat(result(window(ROLE), A, 200).path("totalElements").asInt()).isEqualTo(2);
            jdbc.update("UPDATE guilds SET visibility = 'PUBLIC' WHERE guild_id = ?", GUILD);
            jdbc.update("INSERT INTO personal_role_group_links(owner_player_id,role_id,group_type,group_id) VALUES (?,?,'GUILD',?)", C, FOREIGN_ROLE, GUILD);
            jdbc.update("""
                    INSERT INTO guild_wait_members(guild_id,player_id,requested_at,version,status,type,created_at,updated_at)
                    VALUES (?,?,NOW(6),0,'PENDING','INVITATION',NOW(6),NOW(6))
                    """, GUILD, C);
            assertThat(result(window(FOREIGN_ROLE), C, 200).path("totalElements").asInt()).isZero();
            jdbc.update("DELETE FROM guild_members WHERE guild_id = ? AND player_id = ?", GUILD, A);
            var afterLeave = result(window(ROLE), A, 200);
            assertThat(afterLeave.path("totalElements").asInt()).isEqualTo(1);
            assertThat(afterLeave.toString()).doesNotContain("secret", "Private Guild");
            result(get("/api/v1/guilds/" + GUILD + "/events/88001"), A, 404);
            jdbc.update("UPDATE roles SET status = 'ARCHIVED' WHERE id = ?", ROLE);
            assertThat(result(window(ROLE), A, 200).path("totalElements").asInt()).isEqualTo(1);
            jdbc.update("UPDATE guilds SET status = 'DISBANDED' WHERE guild_id = ?", GUILD);
            assertThat(result(window(ROLE), A, 200).path("totalElements").asInt()).isEqualTo(1);
        }

        @Test @DisplayName("KST 월경계·점 일정·한쪽 시간·미정 일정과 오류를 정확히 구분한다")
        void boundsAndValidation() throws Exception {
            roleEvent(88001, ROLE, "overlap", "2026-10-31 14:00:00", "2026-10-31 15:00:01", "PLANNED");
            roleEvent(88002, ROLE, "at from", "2026-10-31 14:30:00", "2026-10-31 14:30:00", "PLANNED");
            roleEvent(88003, ROLE, "at to", "2026-10-31 16:00:00", null, "PLANNED");
            roleEvent(88004, ROLE, "end only", null, "2026-10-31 15:15:00", "PLANNED");
            roleEvent(88005, ROLE, "unknown", null, null, "PLANNED");
            var timed = result(window(ROLE), A, 200);
            assertThat(timed.path("totalElements").asInt()).isEqualTo(3);
            assertThat(timed.toString()).contains("overlap", "at from", "end only").doesNotContain("at to", "unknown");
            assertThat(result(get(path(ROLE)).param("from", "2026-10-31T23:30:00+09:00")
                    .param("to", "2026-11-01T01:00:00+09:00"), A, 200)).isEqualTo(timed);
            var unknown = result(get(path(ROLE)).param("time", "UNSCHEDULED"), A, 200);
            assertThat(unknown.path("totalElements").asInt()).isEqualTo(1);
            assertThat(unknown.at("/contents/0/startsAt").isNull()).isTrue();
            assertThat(result(get(path(ROLE)), A, 200).path("totalElements").asInt()).isEqualTo(2);
            result(get(path(ROLE)).param("from", TO).param("to", FROM), A, 400);
            result(get(path(ROLE)).param("from", FROM), A, 400);
            result(get(path(ROLE)).param("time", "UNSCHEDULED").param("source", "GUILD"), A, 400);
            result(window(ROLE).param("participating", "true"), A, 400);
            result(window(ROLE).param("size", "51"), A, 400);
            result(window(ROLE).param("page", "1001"), A, 400);
            result(get(path(ROLE)).param("from", FROM).param("to", "2028-11-01T00:00:00Z"), A, 400);
            result(window(ROLE).param("source", "UNKNOWN"), A, 400);
        }
    }

    private void roleEvent(long id, long role, String title, String start, String end, String state) {
        jdbc.update("""
                INSERT INTO role_events(id,player_id,role_id,title,starts_at,ends_at,status,version,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,0,NOW(6),NOW(6))
                """, id, role == FOREIGN_ROLE ? C : A, role, title, start, end, state);
    }
    private void guildEvent(long id, long guild, String title, String start, String end, String state) {
        jdbc.update("""
                INSERT INTO guild_events(id,guild_id,title,starts_at,ends_at,status,created_by_player_id,version,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,0,NOW(6),NOW(6))
                """, id, guild, title, start, end, state, B);
    }
    private long link(long role, long guild) {
        jdbc.update("INSERT INTO personal_role_group_links(owner_player_id,role_id,group_type,group_id) VALUES (?,?,'GUILD',?)", A, role, guild);
        return jdbc.queryForObject("SELECT id FROM personal_role_group_links WHERE role_id = ? AND group_id = ?", Long.class, role, guild);
    }
    private List<Long> counts() {
        var values = List.of("role_events", "guild_events", "guild_event_rsvps", "life_log_records",
                "quest_acceptances", "reward_settlements", "outbox_events")
                .stream().map(table -> jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)).toList();
        var withExp = new java.util.ArrayList<>(values);
        withExp.add(jdbc.queryForObject("SELECT SUM(exp) FROM player WHERE id IN (?, ?, ?)", Long.class, A, B, C));
        return withExp;
    }
    private static String path(long role) { return "/api/v1/roles/" + role + "/schedule"; }
    private static MockHttpServletRequestBuilder window(long role) {
        return get(path(role)).param("from", FROM).param("to", TO);
    }
    private JsonNode result(MockHttpServletRequestBuilder request, long actor, int expected) throws Exception {
        var response = mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwt.createAccessToken(actor, actor)))
                .andExpect(status().is(expected)).andReturn().getResponse();
        return response.getContentAsString().isEmpty() ? json.nullNode() : json.readTree(response.getContentAsString()).path("result");
    }
    private int statusOf(MockHttpServletRequestBuilder request, long actor) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwt.createAccessToken(actor, actor)))
                .andReturn().getResponse().getStatus();
    }

    @TestConfiguration static class IdentityConfig {
        @Bean @Primary CurrentPlayerAccessor contextPlayerAccessor() { return new JwtCurrentPlayerAccessor(); }
        @Bean @Primary Clock scheduleClock() { return Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneOffset.UTC); }
    }
}
