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
import org.junit.jupiter.params.ParameterizedTest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(MemberPersonGuildNoteIntegrationTest.IdentityConfig.class)
@DisplayName("검증된 멤버와 개인 Person 연결 및 길드 개인 메모")
class MemberPersonGuildNoteIntegrationTest {
    private static final long OWNER = 81301, MEMBER = 81302, OUTSIDER = 81303, MEMBER_USER = 91302;
    private static final long GUILD_ONE = 81501, GUILD_TWO = 81502, ROLE = 81401;
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("member_person_note").withUsername("lifeasgame").withPassword("lifeasgame");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean UserAuthApi userAuthApi;

    @BeforeEach void seed() {
        for (String table : List.of("private_guild_member_notes", "role_relations", "persons", "role_party_invitations",
                "role_party_members", "role_parties", "guild_members", "party_members", "guilds", "parties", "roles", "player"))
            jdbc.update("DELETE FROM " + table);
        given(userAuthApi.resolveAuthorization(any())).willReturn(Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        for (long id : List.of(OWNER, MEMBER, OUTSIDER)) jdbc.update("""
                INSERT INTO player (id,user_id,name,level,exp,hp_cur,hp_cap,mp_cur,mp_cap,
                 str_stat,agi_stat,dex_stat,int_stat,vit_stat,luc_stat,extra_stats,status_effects,version,created_at,updated_at)
                VALUES (?,?,'fixture',1,0,100,100,50,50,1,1,1,1,1,1,JSON_OBJECT(),'[]',0,NOW(6),NOW(6))
                """, id, id == MEMBER ? MEMBER_USER : id);
        jdbc.update("INSERT INTO roles(id,player_id,role_type,name,status,version,created_at,updated_at)"
                + " VALUES (?,?,'WORK','Work','ACTIVE',0,NOW(6),NOW(6))", ROLE, OWNER);
        for (long guild : List.of(GUILD_ONE, GUILD_TWO)) {
            jdbc.update("INSERT INTO guilds(guild_id,player_id,leader_player_id,name_original,name_value,code_value,"
                    + "visibility,join_policy,status,max_members,created_at,updated_at)"
                    + " VALUES (?,?,?,'Private Guild','private guild',?,'PRIVATE','INVITE_ONLY','ACTIVE',10,NOW(6),NOW(6))",
                    guild, MEMBER, MEMBER, "g" + guild);
            for (long player : List.of(OWNER, MEMBER)) jdbc.update("INSERT INTO guild_members"
                    + "(guild_id,player_id,role,joined_at,created_at,updated_at)"
                    + " VALUES (?,?,?,NOW(6),NOW(6),NOW(6))", guild, player, player == MEMBER ? "LEADER" : "MEMBER");
        }
        jdbc.update("INSERT INTO parties(party_id,player_id,leader_player_id,name_original,name_value,code_value,"
                + "visibility,join_policy,status,max_members,created_at,updated_at)"
                + " VALUES (?,?,?,'Private Party','private party','p','PRIVATE','INVITE_ONLY','ACTIVE',10,NOW(6),NOW(6))",
                GUILD_ONE, MEMBER, MEMBER);
        for (long player : List.of(OWNER, MEMBER)) jdbc.update("INSERT INTO party_members"
                + "(party_id,player_id,role,joined_at,created_at,updated_at)"
                + " VALUES (?,?,?,NOW(6),NOW(6),NOW(6))", GUILD_ONE, player, player == MEMBER ? "LEADER" : "MEMBER");
        jdbc.update("INSERT INTO role_parties(id,role_id,creator_player_id,leader_player_id,name,status,max_members,version,created_at,updated_at)"
                + " VALUES (?,?,?,?,'Private Role Party','ACTIVE',10,0,NOW(6),NOW(6))", GUILD_ONE, ROLE, OWNER, MEMBER);
        for (long player : List.of(OWNER, MEMBER)) jdbc.update("INSERT INTO role_party_members"
                + "(role_party_id,player_id,joined_at,created_at,updated_at) VALUES (?,?,NOW(6),NOW(6),NOW(6))",
                GUILD_ONE, player);
    }

    @ParameterizedTest @ValueSource(strings = {"GUILD", "PARTY", "ROLE_PARTY"})
    @DisplayName("현재 양쪽 멤버만 User ID로 Person을 연결하고 조회는 변경하지 않는다")
    void linksVerifiedMembers(String type) throws Exception {
        String query = "/api/v1/member-person-links?groupType=" + type + "&groupId=" + GUILD_ONE + "&memberPlayerId=" + MEMBER;
        assertThat(result(get(query), OWNER, 200).path("personId").isNull()).isTrue();
        result(get(query.replace("memberPlayerId=" + MEMBER, "memberPlayerId=" + OUTSIDER)), OWNER, 404);
        result(get(query), OUTSIDER, 404);
        String body = "{\"groupType\":\"" + type + "\",\"groupId\":" + GUILD_ONE
                + ",\"memberPlayerId\":" + MEMBER + ",\"displayName\":\"Confirmed name\"}";
        long id = result(post("/api/v1/member-person-links").content(body), OWNER, 200).path("personId").asLong();
        assertThat(result(post("/api/v1/member-person-links").content(body), OWNER, 200).path("personId").asLong()).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT linked_user_id FROM persons WHERE id = ?", Long.class, id)).isEqualTo(MEMBER_USER);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM persons", Long.class)).isEqualTo(1);
        result(delete("/api/v1/persons/" + id + "/linked-user"), OWNER, 204);
        assertThat(result(get(query), OWNER, 200).path("personId").isNull()).isTrue();
        result(put("/api/v1/member-person-links").content("{\"groupType\":\"" + type
                + "\",\"groupId\":" + GUILD_ONE + ",\"memberPlayerId\":" + MEMBER
                + ",\"personId\":" + id + "}"), OWNER, 200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM persons", Long.class)).isEqualTo(1);
    }

    @Test @DisplayName("선택 충돌·보관 인물·동시 재전송은 원본을 보존한다")
    void conflictsAndConcurrency() throws Exception {
        long selected = result(post("/api/v1/persons").content("{\"displayName\":\"Selected\"}"), OWNER, 201).path("id").asLong();
        String select = "{\"groupType\":\"GUILD\",\"groupId\":" + GUILD_ONE + ",\"memberPlayerId\":" + MEMBER
                + ",\"personId\":" + selected + "}";
        result(put("/api/v1/member-person-links").content(select), OWNER, 200);
        long alternative = result(post("/api/v1/persons").content("{\"displayName\":\"Alternative\"}"), OWNER, 201).path("id").asLong();
        result(put("/api/v1/member-person-links").content(select.replace("personId\":" + selected, "personId\":" + alternative)), OWNER, 409);
        result(delete("/api/v1/persons/" + selected), OWNER, 204);
        result(put("/api/v1/member-person-links").content(select), OWNER, 409);
        assertThat(jdbc.queryForObject("SELECT linked_user_id FROM persons WHERE id = ?", Long.class, selected)).isEqualTo(MEMBER_USER);
    }

    @Test @DisplayName("서로 다른 길드의 동시 신규 연결도 한 Person으로 수렴한다")
    void concurrentCreate() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var sequence = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Long> add = () -> {
                ready.countDown(); start.await(10, TimeUnit.SECONDS);
                long guild = sequence.getAndIncrement() == 0 ? GUILD_ONE : GUILD_TWO;
                String body = "{\"groupType\":\"GUILD\",\"groupId\":" + guild
                        + ",\"memberPlayerId\":" + MEMBER + ",\"displayName\":\"Confirmed\"}";
                return result(post("/api/v1/member-person-links").content(body), OWNER, 200)
                        .path("personId").asLong();
            };
            var one = pool.submit(add);
            var two = pool.submit(add);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(one.get(20, TimeUnit.SECONDS)).isEqualTo(two.get(20, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM persons WHERE owner_player_id = ? AND linked_user_id = ?",
                Long.class, OWNER, MEMBER_USER)).isEqualTo(1);
    }

    @Test @DisplayName("다른 기존 인물을 동시에 선택하면 승자만 연결하고 패자는 409를 받는다")
    void concurrentSelections() throws Exception {
        long one = result(post("/api/v1/persons").content("{\"displayName\":\"One\"}"), OWNER, 201).path("id").asLong();
        long two = result(post("/api/v1/persons").content("{\"displayName\":\"Two\"}"), OWNER, 201).path("id").asLong();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.function.LongFunction<java.util.concurrent.Callable<Integer>> select = person -> () -> {
                ready.countDown(); start.await(5, TimeUnit.SECONDS);
                String body = "{\"groupType\":\"GUILD\",\"groupId\":" + (person == one ? GUILD_ONE : GUILD_TWO)
                        + ",\"memberPlayerId\":" + MEMBER + ",\"personId\":" + person + "}";
                return mvc.perform(put("/api/v1/member-person-links").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", "Bearer " + jwt.createAccessToken(OWNER, OWNER)))
                        .andReturn().getResponse().getStatus();
            };
            var first = pool.submit(select.apply(one));
            var second = pool.submit(select.apply(two));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM persons WHERE linked_user_id = ?", Long.class, MEMBER_USER)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT display_name FROM persons ORDER BY id", String.class)).containsExactly("One", "Two");
    }

    @Test @DisplayName("다른 소유자·다른 신원·대기 멤버를 거부하고 실패한 트랜잭션은 Person을 남기지 않는다")
    void ownershipAndRollback() throws Exception {
        String createBody = "{\"groupType\":\"GUILD\",\"groupId\":" + GUILD_ONE
                + ",\"memberPlayerId\":" + MEMBER + ",\"displayName\":\"Confirmed\"}";
        result(post("/api/v1/member-person-links").content(createBody), OUTSIDER, 404);
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(tx -> {
            try { result(post("/api/v1/member-person-links").content(createBody), OWNER, 200); }
            catch (Exception e) { throw new AssertionError(e); }
            throw new IllegalStateException("rollback fixture");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM persons", Long.class)).isZero();
        long foreign = result(post("/api/v1/persons").content("{\"displayName\":\"Foreign\"}"), MEMBER, 201).path("id").asLong();
        String selection = "{\"groupType\":\"GUILD\",\"groupId\":" + GUILD_ONE
                + ",\"memberPlayerId\":" + MEMBER + ",\"personId\":" + foreign + "}";
        result(put("/api/v1/member-person-links").content(selection), OWNER, 404);
        long self = result(post("/api/v1/member-person-links").content(createBody.replace("memberPlayerId\":" + MEMBER,
                "memberPlayerId\":" + OWNER)), OWNER, 200).path("personId").asLong();
        result(put("/api/v1/member-person-links").content(selection.replace("personId\":" + foreign, "personId\":" + self)), OWNER, 409);
        jdbc.update("INSERT INTO role_party_invitations(role_party_id,inviter_player_id,invitee_player_id,expires_at,status,created_at,updated_at)"
                + " VALUES (?,?,?,NOW(6)+INTERVAL 1 DAY,'PENDING',NOW(6),NOW(6))", GUILD_ONE, MEMBER, OUTSIDER);
        result(post("/api/v1/member-person-links").content(createBody.replace("GUILD", "ROLE_PARTY")
                .replace("memberPlayerId\":" + MEMBER, "memberPlayerId\":" + OUTSIDER)), OWNER, 404);
    }

    @ParameterizedTest @ValueSource(strings = {"unlink", "archive", "leave"})
    @DisplayName("메모 작성 중 신원 해제·보관·탈퇴는 커밋 이후에 적용되고 기존 이력은 보존된다")
    void writesSerializeWithAccessLoss(String change) throws Exception {
        long person = result(post("/api/v1/member-person-links").content("{\"groupType\":\"GUILD\",\"groupId\":"
                + GUILD_ONE + ",\"memberPlayerId\":" + MEMBER + ",\"displayName\":\"Friend\"}"), OWNER, 200)
                .path("personId").asLong();
        var attempted = new CountDownLatch(1);
        var pending = new AtomicReference<Future<JsonNode>>();
        try (var pool = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                try {
                    result(put(notePath(GUILD_ONE)).content("{\"personId\":" + person + ",\"text\":\"history\"}"), OWNER, 200);
                    pending.set(pool.submit(() -> {
                        attempted.countDown();
                        return switch (change) {
                            case "unlink" -> result(delete("/api/v1/persons/" + person + "/linked-user"), OWNER, 204);
                            case "archive" -> result(delete("/api/v1/persons/" + person), OWNER, 204);
                            default -> result(post("/api/v1/guilds/" + GUILD_ONE + "/leave"), OWNER, 200);
                        };
                    }));
                    assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> pending.get().get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                } catch (Exception e) { throw new AssertionError(e); }
            });
            pending.get().get(10, TimeUnit.SECONDS);
        }
        var history = result(get("/api/v1/persons/" + person + "/guild-notes"), OWNER, 200);
        assertThat(history.path("totalElements").asInt()).isEqualTo(1);
        assertThat(history.at("/contents/0/targetMemberPlayerId").asLong()).isEqualTo(MEMBER);
        result(put(notePath(GUILD_ONE)).content("{\"personId\":" + person + ",\"text\":\"blocked\",\"version\":0}"),
                OWNER, change.equals("leave") ? 404 : 409);
    }

    @Test @DisplayName("길드별 메모와 역할 메모를 분리하고 작성자·버전·탈퇴 이력을 지킨다")
    void privateNotesAndHistory() throws Exception {
        long person = result(post("/api/v1/member-person-links").content("{\"groupType\":\"GUILD\",\"groupId\":"
                + GUILD_ONE + ",\"memberPlayerId\":" + MEMBER + ",\"displayName\":\"Friend\"}"), OWNER, 200)
                .path("personId").asLong();
        long relation = result(post("/api/v1/roles/" + ROLE + "/relations").content("{\"personId\":" + person
                + ",\"relationType\":\"FRIEND\",\"roleNotes\":\"role private\"}"), OWNER, 201).path("id").asLong();
        String first = notePath(GUILD_ONE);
        String second = notePath(GUILD_TWO);
        JsonNode note1 = result(put(first).content("{\"personId\":" + person + ",\"text\":\" one \"}"), OWNER, 200);
        JsonNode note2 = result(put(second).content("{\"personId\":" + person + ",\"text\":\"two\"}"), OWNER, 200);
        assertThat(note1.path("text").asText()).isEqualTo("one");
        assertThat(note2.path("text").asText()).isEqualTo("two");
        result(get(first), MEMBER, 404);
        result(put(first).content("{\"personId\":" + person + ",\"text\":\"stolen\"}"), MEMBER, 404);
        result(delete("/api/v1/guild-notes/" + note1.path("id").asLong()), MEMBER, 404);
        result(get("/api/v1/persons/" + person + "/guild-notes"), MEMBER, 404);
        result(put(first).content("{\"personId\":" + person + ",\"text\":\"updated\",\"version\":0}"), OWNER, 200);
        result(put(first).content("{\"personId\":" + person + ",\"text\":\"stale\",\"version\":0}"), OWNER, 409);
        assertThat(result(get("/api/v1/persons/" + person + "/guild-notes").param("size", "1"), OWNER, 200)
                .path("totalElements").asInt()).isEqualTo(2);
        assertThat(result(get("/api/v1/persons/" + person + "/guild-notes").param("keyword", "TWO"), OWNER, 200)
                .path("totalElements").asInt()).isEqualTo(1);
        assertThat(result(get("/api/v1/persons/" + person + "/guild-notes").param("keyword", "%"), OWNER, 200)
                .path("totalElements").asInt()).isZero();
        assertThat(result(get("/api/v1/roles/" + ROLE + "/relations/" + relation), OWNER, 200)
                .path("roleNotes").asText()).isEqualTo("role private");
        result(delete("/api/v1/persons/" + person + "/linked-user"), OWNER, 204);
        assertThat(result(get("/api/v1/persons/" + person + "/guild-notes"), OWNER, 200).path("totalElements").asInt()).isEqualTo(2);
        result(put(first).content("{\"personId\":" + person + ",\"text\":\"blocked\",\"version\":1}"), OWNER, 409);
        jdbc.update("DELETE FROM guild_members WHERE guild_id = ? AND player_id = ?", GUILD_ONE, OWNER);
        JsonNode history = result(get("/api/v1/persons/" + person + "/guild-notes").param("guildId", "" + GUILD_ONE), OWNER, 200);
        assertThat(history.at("/contents/0/availability").asText()).isEqualTo("HISTORY");
        assertThat(history.at("/contents/0/guildName").isNull()).isTrue();
        result(get(first), OWNER, 404);
        result(delete("/api/v1/guild-notes/" + note1.path("id").asLong()), OWNER, 204);
        assertThat(result(get("/api/v1/persons/" + person + "/guild-notes"), OWNER, 200).path("totalElements").asInt()).isEqualTo(1);
    }

    private static String notePath(long guild) { return "/api/v1/guilds/" + guild + "/members/" + MEMBER + "/my-note"; }
    private JsonNode result(MockHttpServletRequestBuilder request, long actor, int expected) throws Exception {
        var response = mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwt.createAccessToken(actor, actor)))
                .andExpect(status().is(expected)).andReturn().getResponse();
        return response.getContentAsString().isEmpty() ? json.nullNode() : json.readTree(response.getContentAsString()).path("result");
    }
    @TestConfiguration static class IdentityConfig {
        @Bean @Primary CurrentPlayerAccessor contextPlayerAccessor() { return new JwtCurrentPlayerAccessor(); }
    }
}
