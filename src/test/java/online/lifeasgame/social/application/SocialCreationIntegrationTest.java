package online.lifeasgame.social.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtCurrentPlayerAccessor;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.command.PartyCommand;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.test.mockmvc.print=NONE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SocialCreationIntegrationTest.IdentityConfig.class)
@DisplayName("Guild/Party 생성 HTTP·H2 영속화 계약")
class SocialCreationIntegrationTest {
    private static final long OWNER = 38001L;
    private static final String BODY = """
            {"name":"New group","code":"%s","visibility":"PUBLIC","joinPolicy":"APPROVAL","maxMembers":1}
            """;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired GuildService guildService;
    @Autowired PartyService partyService;
    @Autowired PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @ValueSource(strings = {"guilds", "parties"})
    @DisplayName("인증된 생성자는 리더 1명으로 저장되고 새 조회에서도 조직과 membership이 일치한다")
    void createsLeader(String groups) throws Exception {
        long id = create(groups);
        mvc.perform(auth(get(path(groups) + "/" + id), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.playerId").value(OWNER))
                .andExpect(jsonPath("$.result.leaderPlayerId").value(OWNER))
                .andExpect(jsonPath("$.result.tags").isEmpty());
        String singular = groups.equals("guilds") ? "guild" : "party";
        assertThat(jdbc.queryForList("SELECT player_id, role FROM " + singular + "_members WHERE "
                + singular + "_id = ?", id)).singleElement().satisfies(member -> {
            assertThat(((Number) member.get("player_id")).longValue()).isEqualTo(OWNER);
            assertThat(member.get("role")).isEqualTo("LEADER");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + singular + "_wait_members WHERE "
                + singular + "_id = ?", Long.class, id)).isZero();
        mvc.perform(auth(post(path(groups) + "/" + id + "/rename")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"), OWNER + 1))
                .andExpect(status().isNotFound());
        mvc.perform(auth(get(path(groups) + "/" + id), OWNER))
                .andExpect(jsonPath("$.result.name").value("New group"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"guilds", "parties"})
    @DisplayName("비인증과 잘못된 생성 입력은 조직을 저장하지 않는다")
    void rejectsInvalidRequests(String groups) throws Exception {
        long before = count(groups);
        String body = BODY.formatted(code());
        mvc.perform(post(path(groups)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        for (String invalid : List.of(body.replace("New group", " "), body.replace("PUBLIC", "INVALID"),
                body.replace("\"maxMembers\":1", "\"maxMembers\":0"),
                body.replace("\"PUBLIC\"", "null"), body.replace("\"APPROVAL\"", "null"),
                body.replace("\"visibility\":\"PUBLIC\",", ""),
                body.replace("\"joinPolicy\":\"APPROVAL\",", ""))) {
            mvc.perform(auth(post(path(groups)).contentType(MediaType.APPLICATION_JSON).content(invalid), OWNER))
                    .andExpect(status().isBadRequest());
        }
        assertThat(count(groups)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"guilds", "parties"})
    @DisplayName("생성 트랜잭션 실패 시 조직과 리더 membership을 함께 롤백한다")
    void rollsBackTogether(String groups) {
        String singular = groups.equals("guilds") ? "guild" : "party";
        long groupsBefore = count(groups);
        long membersBefore = count(singular + "_members");
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            if (groups.equals("guilds")) {
                guildService.create(OWNER, new GuildCommand.Create("New group", code(), null, null, null,
                        "PUBLIC", "APPROVAL", 1));
            } else {
                partyService.create(OWNER, new PartyCommand.Create("New group", code(), null, null, null,
                        "PUBLIC", "APPROVAL", 1));
            }
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("force rollback");
        assertThat(count(groups)).isEqualTo(groupsBefore);
        assertThat(count(singular + "_members")).isEqualTo(membersBefore);
    }

    private long create(String groups) throws Exception {
        var response = mvc.perform(auth(post(path(groups)).param("playerId", "999999")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted(code())), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.leaderPlayerId").value(OWNER))
                .andReturn().getResponse();
        return json.readTree(response.getContentAsString()).path("result").path("id").longValue();
    }

    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private String path(String groups) { return "/api/v1/" + groups; }
    private String code() { return "G" + UUID.randomUUID().toString().replace("-", "").substring(0, 10); }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, long playerId) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(playerId, playerId), null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
    }

    @TestConfiguration
    static class IdentityConfig {
        @Bean @Primary
        CurrentPlayerAccessor identity() { return new JwtCurrentPlayerAccessor(); }
    }
}
