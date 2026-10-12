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

import java.util.Optional;
import java.util.UUID;
import java.util.List;
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
@Import(GuildCreateRosterIntegrationTest.IdentityConfig.class)
@DisplayName("길드 내부 생성과 오프라인 명부의 MySQL·HTTP 계약")
class GuildCreateRosterIntegrationTest {
    private static final long LEADER = 64101, MEMBER = 64102, TARGET = 64103, OUTSIDER = 64104;
    private static final long GUILD = 64101, PARTY = 64101;

    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_guild_roster")
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
        jdbc.update("DELETE FROM guild_group_creation_receipts");
        jdbc.update("DELETE FROM group_roster_invitations");
        jdbc.update("DELETE FROM group_roster_entries");
        jdbc.update("DELETE FROM guild_group_links");
        jdbc.update("DELETE FROM role_party_invitations");
        jdbc.update("DELETE FROM role_party_members");
        jdbc.update("DELETE FROM role_parties");
        jdbc.update("DELETE FROM party_members");
        jdbc.update("DELETE FROM parties");
        jdbc.update("DELETE FROM guild_members");
        jdbc.update("DELETE FROM guilds");
        jdbc.update("DELETE FROM roles WHERE player_id BETWEEN ? AND ?", LEADER, OUTSIDER);
        jdbc.update("DELETE FROM player WHERE id BETWEEN ? AND ?", LEADER, OUTSIDER);
        given(userAuthApi.resolveAuthorization(any())).willReturn(Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        for (long id = LEADER; id <= OUTSIDER; id++) jdbc.update("""
                INSERT INTO player (id,user_id,name,level,exp,hp_cur,hp_cap,mp_cur,mp_cap,
                  str_stat,agi_stat,dex_stat,int_stat,vit_stat,luc_stat,extra_stats,status_effects,
                  version,created_at,updated_at)
                VALUES (?,?,'fixture',1,0,100,100,50,50,1,1,1,1,1,1,JSON_OBJECT(),'[]',0,NOW(6),NOW(6))
                """, id, id);
        jdbc.update("""
                INSERT INTO guilds (guild_id,player_id,leader_player_id,name_original,name_value,code_value,
                  visibility,join_policy,status,max_members,created_at,updated_at)
                VALUES (?,?,?,'guild','guild','ROSTER-GUILD','PRIVATE','APPROVAL','ACTIVE',3,NOW(6),NOW(6))
                """, GUILD, LEADER, LEADER);
        for (long id : new long[]{LEADER, MEMBER}) jdbc.update("""
                INSERT INTO guild_members (guild_id,player_id,role,joined_at,created_at,updated_at)
                VALUES (?,?,?,NOW(6),NOW(6),NOW(6))
                """, GUILD, id, id == LEADER ? "LEADER" : "MEMBER");
        jdbc.update("""
                INSERT INTO parties (party_id,player_id,leader_player_id,name_original,name_value,code_value,
                  visibility,join_policy,status,max_members,created_at,updated_at)
                VALUES (?,?,?,'party','party','ROSTER-PARTY','PRIVATE','INVITE_ONLY','ACTIVE',2,NOW(6),NOW(6))
                """, PARTY, LEADER, LEADER);
        jdbc.update("""
                INSERT INTO party_members (party_id,player_id,role,joined_at,created_at,updated_at)
                VALUES (?,?, 'LEADER',NOW(6),NOW(6),NOW(6))
                """, PARTY, LEADER);
        jdbc.update("""
                INSERT INTO roles (player_id,role_type,name,status,created_at,updated_at,version)
                VALUES (?,'WORK','private role','ACTIVE',NOW(6),NOW(6),0)
                """, LEADER);
    }

    @Test
    @DisplayName("현재 길드 멤버가 새 Party를 만들면 리더는 즉시, 일반 멤버는 승인 대기하고 재전송은 중복 생성하지 않는다")
    void createsInsideGuild() throws Exception {
        String base = "/api/v1/guilds/" + GUILD + "/groups";
        String key = UUID.randomUUID().toString();
        String body = "{\"clientRequestId\":\"" + key + "\",\"groupType\":\"PARTY\",\"displayName\":\"public label\","
                + "\"party\":{\"name\":\"private name\",\"code\":\"NEW-PARTY\",\"visibility\":\"PRIVATE\","
                + "\"joinPolicy\":\"INVITE_ONLY\",\"maxMembers\":2}}";
        JsonNode created = result(mvc.perform(auth(post(base), MEMBER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(created.path("linkStatus").asText()).isEqualTo("PENDING");
        mvc.perform(auth(post(base), MEMBER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.groupId").value(created.path("groupId").asLong()));
        mvc.perform(auth(post(base), MEMBER).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("public label", "different label")))
                .andExpect(status().isConflict());
        mvc.perform(auth(get("/api/v1/guilds/" + GUILD + "/group-links"), LEADER))
                .andExpect(jsonPath("$.result.totalElements").value(0))
                .andExpect(jsonPath("$.result.capabilities.canStartCreateGroup").value(true));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parties WHERE code_value='NEW-PARTY'", Long.class)).isEqualTo(1);
        mvc.perform(auth(post("/api/v1/guilds/" + GUILD + "/group-links/" + created.path("linkId").asLong() + "/approve"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"public label\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("ACTIVE"));
        mvc.perform(auth(post(base), OUTSIDER).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace(key, UUID.randomUUID().toString()))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("길드와 Party 명부는 가입과 분리되고 지정 계정의 수락만 회원 가입과 연결을 원자적으로 수행한다")
    void acceptsRosterInvitation() throws Exception {
        String guildRoster = "/api/v1/guilds/" + GUILD + "/roster";
        long guildEntry = result(mvc.perform(auth(post(guildRoster), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"same name\",\"groupRoleLabel\":\"friend\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("rosterEntryId").asLong();
        mvc.perform(auth(post(guildRoster), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"same name\"}"))
                .andExpect(status().isCreated());
        mvc.perform(auth(get(guildRoster), MEMBER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.capabilities.canManageRoster").value(false));
        mvc.perform(auth(get(guildRoster), OUTSIDER)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_members WHERE guild_id=?", Long.class, GUILD)).isEqualTo(2);
        long invitation = result(mvc.perform(auth(post(guildRoster + "/" + guildEntry + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + TARGET + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        mvc.perform(auth(get("/api/v1/roster-invitations/mine"), TARGET)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[0].rosterDisplayName").value("same name"))
                .andExpect(jsonPath("$.result.contents[0].membershipWillBeCreated").value(true));
        mvc.perform(auth(post("/api/v1/roster-invitations/" + invitation + "/accept"), OUTSIDER))
                .andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/v1/roster-invitations/" + invitation + "/accept"), TARGET))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.memberStatus").value("ACTIVE"));
        mvc.perform(auth(post("/api/v1/roster-invitations/" + invitation + "/accept"), TARGET))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_members WHERE guild_id=?", Long.class, GUILD)).isEqualTo(3);
        mvc.perform(auth(post("/api/v1/guilds/" + GUILD + "/leave"), TARGET)).andExpect(status().isOk());
        mvc.perform(auth(get(guildRoster), LEADER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.contents[1].memberStatus").value("LEFT"));
        mvc.perform(auth(post("/api/v1/roster-invitations/" + invitation + "/accept"), TARGET))
                .andExpect(status().isConflict());

        String partyRoster = "/api/v1/parties/" + PARTY + "/roster";
        long partyEntry = result(mvc.perform(auth(post(partyRoster), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"offline\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("rosterEntryId").asLong();
        long partyInvitation = result(mvc.perform(auth(post(partyRoster + "/" + partyEntry + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + MEMBER + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        mvc.perform(auth(post("/api/v1/roster-invitations/" + partyInvitation + "/accept"), MEMBER))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM party_members WHERE party_id=?", Long.class, PARTY)).isEqualTo(2);
    }

    @Test
    @DisplayName("내 활성 Role로 만든 RoleParty만 즉시 연결되고 유효하지 않은 입력은 모임을 남기지 않는다")
    void createsRolePartyAndRollsBack() throws Exception {
        long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE player_id=?", Long.class, LEADER);
        String base = "/api/v1/guilds/" + GUILD + "/groups";
        String body = "{\"clientRequestId\":\"" + UUID.randomUUID() + "\",\"groupType\":\"ROLE_PARTY\","
                + "\"displayName\":\"shared label\",\"roleParty\":{\"roleId\":" + roleId
                + ",\"name\":\"role group\",\"maxMembers\":2}}";
        mvc.perform(auth(post(base), LEADER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.result.linkStatus").value("ACTIVE"));
        jdbc.update("""
                INSERT INTO roles (player_id,role_type,name,status,created_at,updated_at,version)
                VALUES (?,'WORK','member role','ACTIVE',NOW(6),NOW(6),0)
                """, MEMBER);
        long memberRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE player_id=?", Long.class, MEMBER);
        mvc.perform(auth(post(base), MEMBER).contentType(MediaType.APPLICATION_JSON)
                .content(body.replaceFirst("[0-9a-f-]{36}", UUID.randomUUID().toString())
                        .replace("\"roleId\":" + roleId, "\"roleId\":" + memberRoleId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.result.linkStatus").value("PENDING"));
        mvc.perform(auth(post(base), MEMBER).contentType(MediaType.APPLICATION_JSON)
                .content(body.replaceFirst("[0-9a-f-]{36}", UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        String invalidLabel = "x".repeat(121);
        mvc.perform(auth(post(base), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content(body.replaceFirst("[0-9a-f-]{36}", UUID.randomUUID().toString())
                        .replace("shared label", invalidLabel)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_parties", Long.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("이미 가입한 대상은 정원을 소비하지 않고 연결하며 버전 충돌과 리더 교체는 기존 상태를 보존한다")
    void existingMemberAndLeaderChange() throws Exception {
        String roster = "/api/v1/guilds/" + GUILD + "/roster";
        long entry = result(mvc.perform(auth(post(roster), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"known member\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("rosterEntryId").asLong();
        mvc.perform(auth(patch(roster + "/" + entry), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"new label\",\"version\":0}"))
                .andExpect(status().isOk());
        mvc.perform(auth(patch(roster + "/" + entry), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"stale\",\"version\":0}"))
                .andExpect(status().isConflict());
        long invitation = result(mvc.perform(auth(post(roster + "/" + entry + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + MEMBER + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        mvc.perform(auth(post("/api/v1/roster-invitations/" + invitation + "/accept"), MEMBER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.memberStatus").value("ACTIVE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_members WHERE guild_id=?", Long.class, GUILD)).isEqualTo(2);
        long second = result(mvc.perform(auth(post(roster), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"another\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("rosterEntryId").asLong();
        long conflicting = result(mvc.perform(auth(post(roster + "/" + second + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + MEMBER + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        mvc.perform(auth(post("/api/v1/roster-invitations/" + conflicting + "/accept"), MEMBER))
                .andExpect(status().isConflict());
        mvc.perform(auth(post(roster + "/invitations/" + conflicting + "/cancel"), LEADER))
                .andExpect(status().isOk());
        long pending = result(mvc.perform(auth(post(roster + "/" + second + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + TARGET + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        mvc.perform(auth(post("/api/v1/guilds/" + GUILD + "/transfer-leader"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"toPlayerId\":" + MEMBER + "}"))
                .andExpect(status().isOk());
        mvc.perform(auth(post("/api/v1/roster-invitations/" + pending + "/accept"), TARGET))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT status FROM group_roster_invitations WHERE id=?", String.class, pending))
                .isEqualTo("CANCELED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_members WHERE guild_id=?", Long.class, GUILD)).isEqualTo(2);
    }

    @Test
    @DisplayName("마지막 자리에서 명부 수락과 기존 공개 가입이 경합해도 한 명만 가입한다")
    void serializesLastSeatWithExistingJoin() throws Exception {
        jdbc.update("UPDATE guilds SET join_policy='OPEN',visibility='PUBLIC' WHERE guild_id=?", GUILD);
        String roster = "/api/v1/guilds/" + GUILD + "/roster";
        long entry = result(mvc.perform(auth(post(roster), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"offline\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("rosterEntryId").asLong();
        long invitation = result(mvc.perform(auth(post(roster + "/" + entry + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + TARGET + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var rosterAccept = pool.submit(() -> { start.await(); return mvc.perform(auth(post(
                    "/api/v1/roster-invitations/" + invitation + "/accept"), TARGET))
                    .andReturn().getResponse().getStatus(); });
            var ordinaryJoin = pool.submit(() -> { start.await(); return mvc.perform(auth(post(
                    "/api/v1/guilds/" + GUILD + "/request-join"), OUTSIDER)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andReturn().getResponse().getStatus(); });
            start.countDown();
            assertThat(List.of(rosterAccept.get(15, TimeUnit.SECONDS), ordinaryJoin.get(15, TimeUnit.SECONDS)))
                    .contains(200).anyMatch(code -> code == 400 || code == 409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_members WHERE guild_id=?", Long.class, GUILD)).isEqualTo(3);
        Long linked = jdbc.queryForObject("SELECT linked_player_id FROM group_roster_entries WHERE id=?", Long.class, entry);
        assertThat(linked == null || linked == TARGET).isTrue();
    }

    @Test
    @DisplayName("삭제·만료·취소된 명부 초대는 가입시키지 않고 기존 회원수를 유지한다")
    void rejectsClosedInvitations() throws Exception {
        String roster = "/api/v1/guilds/" + GUILD + "/roster";
        long entry = result(mvc.perform(auth(post(roster), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"offline\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("rosterEntryId").asLong();
        long invitation = result(mvc.perform(auth(post(roster + "/" + entry + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + TARGET + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        jdbc.update("UPDATE group_roster_invitations SET expires_at=DATE_SUB(NOW(6),INTERVAL 1 SECOND) WHERE id=?", invitation);
        mvc.perform(auth(post("/api/v1/roster-invitations/" + invitation + "/accept"), TARGET))
                .andExpect(status().isConflict());
        long renewed = result(mvc.perform(auth(post(roster + "/" + entry + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + TARGET + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        assertThat(renewed).isNotEqualTo(invitation);
        mvc.perform(auth(post(roster + "/invitations/" + renewed + "/cancel"), LEADER))
                .andExpect(status().isOk());
        mvc.perform(auth(post("/api/v1/roster-invitations/" + renewed + "/accept"), TARGET))
                .andExpect(status().isConflict());
        long third = result(mvc.perform(auth(post(roster + "/" + entry + "/invitations"), LEADER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetPlayerId\":" + TARGET + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("invitationId").asLong();
        mvc.perform(auth(delete(roster + "/" + entry), LEADER).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}"))
                .andExpect(status().isNoContent());
        mvc.perform(auth(post("/api/v1/roster-invitations/" + third + "/accept"), TARGET))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM guild_members WHERE guild_id=?", Long.class, GUILD)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_roster_invitations WHERE status='ACCEPTED'", Long.class)).isZero();
    }

    private JsonNode result(String body) throws Exception { return json.readTree(body).path("result"); }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, long playerId) {
        return request.header("Authorization", "Bearer " + jwt.createAccessToken(playerId, playerId));
    }

    @TestConfiguration
    static class IdentityConfig {
        @Bean @Primary CurrentPlayerAccessor playerAccessor() { return new JwtCurrentPlayerAccessor(); }
    }
}
