package online.lifeasgame.social.application;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "migration-test"})
@Import(RolePersonGroupContextIntegrationTest.IdentityConfig.class)
@DisplayName("인물 역할 맥락과 개인 모임 연결의 HTTP·MySQL 계약")
class RolePersonGroupContextIntegrationTest {
    private static final long A = 70301, B = 70302, C = 70303;
    private static final long ROLE = 70401, OTHER_ROLE = 70402, FOREIGN_ROLE = 70403, GROUP = 70501;
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("role_person_context").withUsername("lifeasgame").withPassword("lifeasgame");
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
        for (String table : List.of("personal_role_group_links", "role_relations", "persons", "role_party_invitations",
                "role_party_members", "role_parties", "guild_members", "party_members", "guilds", "parties", "roles", "player"))
            jdbc.update("DELETE FROM " + table);
        given(userAuthApi.resolveAuthorization(any())).willReturn(Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        for (long id : List.of(A, B, C)) jdbc.update("""
                INSERT INTO player (id,user_id,name,level,exp,hp_cur,hp_cap,mp_cur,mp_cap,
                 str_stat,agi_stat,dex_stat,int_stat,vit_stat,luc_stat,extra_stats,status_effects,version,created_at,updated_at)
                VALUES (?,?,'fixture',1,0,100,100,50,50,1,1,1,1,1,1,JSON_OBJECT(),'[]',0,NOW(6),NOW(6))
                """, id, id);
        for (long id : List.of(ROLE, OTHER_ROLE, FOREIGN_ROLE)) jdbc.update("""
                INSERT INTO roles (id,player_id,role_type,name,status,version,created_at,updated_at)
                VALUES (?,?,'WORK',?,'ACTIVE',0,NOW(6),NOW(6))
                """, id, id == FOREIGN_ROLE ? C : A, id == ROLE ? "Work%literal" : "Home");
        for (String type : List.of("guild", "party")) {
            String table = type.equals("guild") ? "guilds" : "parties";
            jdbc.update("INSERT INTO " + table + " (" + type + "_id,player_id,leader_player_id,name_original,name_value,"
                    + "code_value,visibility,join_policy,status,max_members,created_at,updated_at)"
                    + " VALUES (?,?,?,'Secret%group','secret%group',?,'PRIVATE','INVITE_ONLY','ACTIVE',10,NOW(6),NOW(6))", GROUP, B, B, type);
            for (long member : List.of(A, B)) jdbc.update("INSERT INTO " + type + "_members (" + type
                    + "_id,player_id,role,joined_at,created_at,updated_at) VALUES (?,?,?,NOW(6),NOW(6),NOW(6))",
                    GROUP, member, member == B ? "LEADER" : "MEMBER");
        }
        jdbc.update("""
                INSERT INTO role_parties (id,role_id,creator_player_id,leader_player_id,name,status,max_members,version,created_at,updated_at)
                VALUES (?,?,?,?,'Secret%group','ACTIVE',10,0,NOW(6),NOW(6))
                """, GROUP, ROLE, A, B);
        for (long member : List.of(A, B)) jdbc.update("""
                INSERT INTO role_party_members (role_party_id,player_id,joined_at,created_at,updated_at)
                VALUES (?,?,NOW(6),NOW(6),NOW(6))
                """, GROUP, member);
    }

    @Nested @DisplayName("한 인물의 역할별 관계를 조회·수정하면")
    class PersonContexts {
        @Test @DisplayName("메모·공통 프로필을 분리하고 SQL 검색·페이지·보관 이력을 유지한다")
        void separateNotesAndHistory() throws Exception {
            JsonNode person = result(post("/api/v1/persons").content("""
                    {"displayName":"Alice","notes":"common","profile":{"nickname":"unchanged"}}
                    """), A, 201);
            long personId = person.path("id").asLong();
            long r1 = relation(ROLE, personId, "A memo");
            long r2 = relation(OTHER_ROLE, personId, "B memo");
            String path = "/api/v1/persons/" + personId + "/role-contexts";
            var first = result(get(path).param("size", "1"), A, 200);
            assertThat(first.path("totalElements").asInt()).isEqualTo(2);
            assertThat(first.at("/contents/0/relationId").asLong()).isEqualTo(r1);
            assertThat(result(get(path).param("size", "1").param("page", "1"), A, 200)
                    .at("/contents/0/relationId").asLong()).isEqualTo(r2);
            assertThat(result(get(path).param("keyword", "%"), A, 200).path("totalElements").asInt()).isEqualTo(1);
            result(put("/api/v1/roles/" + ROLE + "/relations/" + r1)
                    .content("{\"relationType\":\"FRIEND\",\"roleNotes\":\"A changed\"}"), A, 200);
            assertThat(result(get("/api/v1/persons/" + personId), A, 200)).isEqualTo(person);
            assertThat(result(get("/api/v1/roles/" + OTHER_ROLE + "/relations/" + r2), A, 200)
                    .path("roleNotes").asText()).isEqualTo("B memo");
            result(get(path), C, 404);
            result(get(path).param("size", "101"), A, 400);
            result(get(path).param("page", "-1"), A, 400);
            result(delete("/api/v1/roles/" + ROLE), A, 204);
            assertThat(result(get(path), A, 200).path("totalElements").asInt()).isEqualTo(1);
            result(delete("/api/v1/roles/" + OTHER_ROLE + "/relations/" + r2), A, 204);
            assertThat(result(get(path), A, 200).path("totalElements").asInt()).isZero();
            result(delete("/api/v1/persons/" + personId), A, 204);
            var history = result(get(path).param("includeArchived", "true"), A, 200);
            assertThat(history.path("totalElements").asInt()).isEqualTo(2);
            assertThat(history.at("/contents/0/roleStatus").asText()).isEqualTo("ARCHIVED");
            assertThat(history.at("/contents/1/relationStatus").asText()).isEqualTo("ARCHIVED");
            result(put("/api/v1/roles/" + ROLE + "/relations/" + r1)
                    .content("{\"relationType\":\"FRIEND\",\"roleNotes\":\"forbidden\"}"), A, 409);
            assertThat(jdbc.queryForObject("SELECT role_notes FROM role_relations WHERE id = ?", String.class, r1)).isEqualTo("A changed");
        }
    }

    @Nested @DisplayName("개인 Role에 모임을 연결하면")
    class GroupLinks {
        @ParameterizedTest @ValueSource(strings = {"GUILD", "PARTY", "ROLE_PARTY"})
        @DisplayName("일반 멤버도 연결하고 재시도는 같은 ID이며 탈퇴 후 비공개 정보 없이 해제한다")
        void membershipAndCleanup(String type) throws Exception {
            long link = link(ROLE, type, A, 200).path("linkId").asLong();
            assertThat(link(ROLE, type, A, 200).path("linkId").asLong()).isEqualTo(link);
            long second = link(OTHER_ROLE, type, A, 200).path("linkId").asLong();
            assertThat(second).isNotEqualTo(link);
            link(FOREIGN_ROLE, type, A, 404);
            link(FOREIGN_ROLE, type, C, 404);
            var candidate = result(get("/api/v1/roles/" + ROLE + "/group-link-candidates").param("groupType", type), A, 200);
            assertThat(candidate.path("totalElements").asInt()).isEqualTo(1);
            assertThat(candidate.at("/contents/0/memberRole").asText()).isEqualTo("MEMBER");
            result(post(groupPath(type) + "/leave"), A, type.equals("ROLE_PARTY") ? 204 : 200);
            var hidden = result(get(links(ROLE)), A, 200).at("/contents/0");
            assertThat(hidden.path("access").asText()).isEqualTo("UNAVAILABLE");
            assertThat(hidden.path("group").isNull()).isTrue();
            assertThat(hidden.toString()).doesNotContain("Secret");
            assertThat(result(get(links(ROLE)).param("keyword", "Secret"), A, 200).path("totalElements").asInt()).isZero();
            assertThat(result(get("/api/v1/roles/" + ROLE + "/group-link-candidates").param("groupType", type), A, 200)
                    .path("totalElements").asInt()).isZero();
            result(get(groupPath(type)), A, 404);
            link(ROLE, type, A, 404);
            for (int i = 0; i < 2; i++) result(delete(links(ROLE) + "/" + link), A, 204);
            assertThat(result(get(links(OTHER_ROLE)), A, 200).path("totalElements").asInt()).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_parties WHERE role_id = ?", Long.class, ROLE)).isEqualTo(1);
            result(get(groupPath(type)), B, 200);
        }

        @ParameterizedTest @ValueSource(strings = {"GUILD", "PARTY", "ROLE_PARTY"})
        @DisplayName("리더 이전은 현재 권한만 바꾸고 해산 후 링크가 권한을 부여하지 않는다")
        void leadershipAndDisband(String type) throws Exception {
            long link = link(ROLE, type, A, 200).path("linkId").asLong();
            result(post(groupPath(type) + "/transfer-leader").content("{\"fromLeaderPlayerId\":" + B + ",\"toPlayerId\":" + A + "}"), B, 200);
            assertThat(result(get(links(ROLE)), A, 200).at("/contents/0/group/memberRole").asText()).isEqualTo("LEADER");
            result(post(groupPath(type) + "/disband"), A, 200);
            assertThat(result(get(links(ROLE)), A, 200).at("/contents/0/group").isNull()).isTrue();
            link(ROLE, type, A, 404);
            result(delete(links(ROLE) + "/" + link), A, 204);
        }

        @Test @DisplayName("종류별 같은 숫자 ID를 구분하고 필터·페이지·읽기 무변경을 보장한다")
        void pagingAndNoSideEffects() throws Exception {
            long exp = jdbc.queryForObject("SELECT exp FROM player WHERE id = ?", Long.class, A);
            long members = countMembers();
            var facts = sideEffects();
            for (String type : List.of("GUILD", "PARTY", "ROLE_PARTY")) link(ROLE, type, A, 200);
            var page = result(get(links(ROLE)).param("size", "1"), A, 200);
            assertThat(page.path("totalElements").asInt()).isEqualTo(3);
            assertThat(page.at("/contents/0/groupType").asText()).isEqualTo("ROLE_PARTY");
            assertThat(result(get(links(ROLE)).param("size", "1").param("page", "1"), A, 200)
                    .at("/contents/0/groupType").asText()).isEqualTo("PARTY");
            assertThat(result(get(links(ROLE)).param("groupType", "GUILD").param("keyword", "%"), A, 200)
                    .path("totalElements").asInt()).isEqualTo(1);
            assertThat(result(get(links(ROLE)).param("keyword", "_"), A, 200).path("totalElements").asInt()).isZero();
            result(get(links(ROLE)).param("groupType", "BOGUS"), A, 400);
            result(get(links(ROLE)).param("size", "0"), A, 400);
            result(get(links(ROLE)).param("page", "-1"), A, 400);
            result(get(links(ROLE)), C, 404);
            mvc.perform(get(links(ROLE))).andExpect(status().isUnauthorized());
            assertThat(countMembers()).isEqualTo(members);
            assertThat(sideEffects()).isEqualTo(facts);
            assertThat(jdbc.queryForObject("SELECT exp FROM player WHERE id = ?", Long.class, A)).isEqualTo(exp);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_party_invitations", Long.class)).isZero();
            long link = page.at("/contents/0/linkId").asLong();
            result(delete("/api/v1/roles/" + ROLE), A, 204);
            link(ROLE, "GUILD", A, 409);
            result(delete(links(ROLE) + "/" + link), A, 204);
            assertThat(result(get(links(ROLE)), A, 200).path("totalElements").asInt()).isEqualTo(2);
        }

        @Test @DisplayName("동시 추가는 하나의 행으로 수렴하고 DB가 소유권 FK·중복을 강제한다")
        void concurrencyAndConstraints() throws Exception {
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            try (var pool = Executors.newFixedThreadPool(2)) {
                java.util.concurrent.Callable<Long> add = () -> { ready.countDown(); start.await(10, TimeUnit.SECONDS);
                    return link(ROLE, "GUILD", A, 200).path("linkId").asLong(); };
                var one = pool.submit(add); var two = pool.submit(add);
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
                assertThat(one.get(20, TimeUnit.SECONDS)).isEqualTo(two.get(20, TimeUnit.SECONDS));
            }
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM personal_role_group_links", Long.class)).isEqualTo(1);
            assertThatThrownBy(() -> jdbc.update("INSERT INTO personal_role_group_links(owner_player_id,role_id,group_type,group_id) VALUES (?,?, 'GUILD',?)", A, FOREIGN_ROLE, GROUP))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("INSERT INTO personal_role_group_links(owner_player_id,role_id,group_type,group_id) VALUES (?,?, 'GUILD',?)", A, ROLE, GROUP))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> {
                jdbc.update("INSERT INTO personal_role_group_links(owner_player_id,role_id,group_type,group_id) VALUES (?,?, 'PARTY',?)", A, ROLE, GROUP);
                throw new IllegalStateException("rollback");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM personal_role_group_links", Long.class)).isEqualTo(1);
        }

        @Test @DisplayName("초대 대기·공개 비멤버는 후보가 아니며 강퇴 뒤 비공개 제목을 숨긴다")
        void pendingAndKick() throws Exception {
            jdbc.update("""
                    INSERT INTO role_party_invitations(role_party_id,inviter_player_id,invitee_player_id,expires_at,status,created_at,updated_at)
                    VALUES (?,?,?,NOW(6) + INTERVAL 1 DAY,'PENDING',NOW(6),NOW(6))
                    """, GROUP, B, C);
            link(FOREIGN_ROLE, "ROLE_PARTY", C, 404);
            jdbc.update("UPDATE guilds SET visibility = 'PUBLIC' WHERE guild_id = ?", GROUP);
            link(FOREIGN_ROLE, "GUILD", C, 404);
            link(ROLE, "PARTY", A, 200);
            result(post(groupPath("PARTY") + "/kick").content("{\"targetPlayerId\":" + A + "}"), B, 200);
            assertThat(result(get(links(ROLE)), A, 200).at("/contents/0/group").isNull()).isTrue();
        }
    }

    private List<Long> sideEffects() {
        return List.of("life_log_records", "quest_signal_receipts", "reward_settlements", "guild_wait_members", "party_wait_members")
                .stream().map(table -> jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)).toList();
    }
    private long countMembers() { return jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM guild_members) + (SELECT COUNT(*) FROM party_members) + (SELECT COUNT(*) FROM role_party_members)", Long.class); }
    private long relation(long role, long person, String notes) throws Exception {
        return result(post("/api/v1/roles/" + role + "/relations")
                .content("{\"personId\":" + person + ",\"relationType\":\"FRIEND\",\"roleNotes\":\"" + notes + "\"}"), A, 201).path("id").asLong();
    }
    private JsonNode link(long role, String type, long actor, int status) throws Exception {
        return result(post(links(role)).content("{\"groupType\":\"" + type + "\",\"groupId\":" + GROUP + "}"), actor, status);
    }
    private static String links(long role) { return "/api/v1/roles/" + role + "/group-links"; }
    private static String groupPath(String type) { return "/api/v1/" + switch (type) { case "GUILD" -> "guilds"; case "PARTY" -> "parties"; default -> "role-parties"; } + "/" + GROUP; }
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
