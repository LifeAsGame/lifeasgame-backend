package online.lifeasgame.character.api.player;

import online.lifeasgame.character.application.GrowthQueryService;
import online.lifeasgame.character.application.PlayerFacade;
import online.lifeasgame.character.application.PlayerQueryService;
import online.lifeasgame.character.application.PlayerService;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.GrowthResult;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Player Growth API contract")
class PlayerGrowthApiContractTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-13T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrowthQueryService growthQueryService;

    @MockitoBean
    private PlayerFacade playerFacade;

    @MockitoBean
    private PlayerQueryService playerQueryService;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("POST /api/v1/players/register")
    class RegisterPlayer {

        @Test
        @DisplayName("기존 request/response와 201 Location 계약을 보존한다")
        void preservesPublicContract() throws Exception {
            given(playerFacade.linkStart(new PlayerCommand.Register(
                    "새 플레이어",
                    "MALE"
            ))).willReturn(new PlayerResult.CreatedWithToken(
                    331L,
                    "access-token",
                    "refresh-token"
            ));

            mockMvc.perform(post("/api/v1/players/register")
                            .with(authentication(userAuthentication()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "새 플레이어",
                                      "gender": "MALE"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(
                            "Location",
                            "/api/v1/players/331"
                    ))
                    .andExpect(jsonPath("$.result.id").value(331))
                    .andExpect(jsonPath("$.result.accessToken")
                            .value("access-token"))
                    .andExpect(jsonPath("$.result.refreshToken")
                            .value("refresh-token"));

            verify(playerFacade).linkStart(new PlayerCommand.Register(
                    "새 플레이어",
                    "MALE"
            ));
        }

        @Test
        @DisplayName("인증이 없으면 401이다")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(post("/api/v1/players/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"새 플레이어","gender":"MALE"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/players/growth")
    class GetGrowth {

        @Test
        @DisplayName("identity parameter 없이 current Player endpoint를 노출한다")
        void exposesParameterlessCurrentPlayerEndpoint() throws Exception {
            assertThat(PlayerController.class.getDeclaredMethod("growth").getParameterCount())
                    .isZero();
        }

        @Test
        @DisplayName("authoritative current와 recent EXP 필드를 정확히 반환하고 rewardLineId는 숨긴다")
        void returnsPopulatedExactShape() throws Exception {
            given(growthQueryService.getCurrentGrowth()).willReturn(populatedGrowth());

            mockMvc.perform(get("/api/v1/players/growth")
                            .with(authentication(playerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.current.level").value(3))
                    .andExpect(jsonPath("$.result.current.exp").value(250))
                    .andExpect(jsonPath("$.result.current.str").value(2))
                    .andExpect(jsonPath("$.result.current.agi").value(3))
                    .andExpect(jsonPath("$.result.current.dex").value(4))
                    .andExpect(jsonPath("$.result.current.intel").value(5))
                    .andExpect(jsonPath("$.result.current.vit").value(6))
                    .andExpect(jsonPath("$.result.current.luc").value(7))
                    .andExpect(jsonPath("$.result.current.extraStats.sociability").value(8))
                    .andExpect(jsonPath("$.result.current.representativeTitleId").value(9))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].changeId").value(10))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].requestedExp").value(100))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].appliedExp").value(80))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].leftoverExp").value(20))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].beforeLevel").value(2))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].afterLevel").value(3))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].beforeTotalExp").value(170))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].afterTotalExp").value(250))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].occurredAt")
                            .value("2026-08-13T00:00:00Z"))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].sourceType")
                            .value("QUEST_COMPLETION"))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].sourceId").value(2640))
                    .andExpect(jsonPath("$.result.recentExpChanges[0].rewardLineId")
                            .doesNotExist());
        }

        @Test
        @DisplayName("history가 없으면 recentExpChanges 빈 배열을 반환한다")
        void returnsEmptyHistoryArray() throws Exception {
            given(growthQueryService.getCurrentGrowth()).willReturn(new GrowthResult.Overview(
                    new GrowthResult.Current(1, 0, 1, 1, 1, 1, 1, 1, Map.of(), null),
                    List.of()
            ));

            mockMvc.perform(get("/api/v1/players/growth")
                            .with(authentication(playerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.recentExpChanges").isArray())
                    .andExpect(jsonPath("$.result.recentExpChanges").isEmpty())
                    .andExpect(jsonPath("$.result.current.representativeTitleId").doesNotExist());
        }

        @Test
        @DisplayName("missing provenance는 null 필드로 반환한다")
        void returnsNullableProvenance() throws Exception {
            GrowthResult.Overview growth = populatedGrowth();
            GrowthResult.RecentExpChange change = growth.recentExpChanges().getFirst();
            given(growthQueryService.getCurrentGrowth()).willReturn(new GrowthResult.Overview(
                    growth.current(),
                    List.of(new GrowthResult.RecentExpChange(
                            change.changeId(), change.requestedExp(), change.appliedExp(),
                            change.leftoverExp(), change.beforeLevel(), change.afterLevel(),
                            change.beforeTotalExp(), change.afterTotalExp(), change.occurredAt(),
                            null, null
                    ))
            ));

            mockMvc.perform(get("/api/v1/players/growth")
                            .with(authentication(playerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.recentExpChanges[0].sourceType").doesNotExist())
                    .andExpect(jsonPath("$.result.recentExpChanges[0].sourceId").doesNotExist());
        }

        @Test
        @DisplayName("인증이 없으면 401이다")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/players/growth"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("기존 Player 조회")
    class ExistingPlayerApi {

        @Test
        @DisplayName("GET /api/v1/players response shape를 보존한다")
        void preservesPlayerResponse() throws Exception {
            given(playerQueryService.getPlayerInfo()).willReturn(new PlayerResult.PlayerInfo(
                    1L, "player", "MALE", null, 1, 0,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    Map.of(), List.of(), null
            ));

            mockMvc.perform(get("/api/v1/players")
                            .with(authentication(playerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.playerId").value(1))
                    .andExpect(jsonPath("$.result.name").value("player"))
                    .andExpect(jsonPath("$.result.level").value(1))
                    .andExpect(jsonPath("$.result.exp").value(0));
        }
    }

    private GrowthResult.Overview populatedGrowth() {
        return new GrowthResult.Overview(
                new GrowthResult.Current(
                        3, 250, 2, 3, 4, 5, 6, 7,
                        Map.of("sociability", 8), 9L
                ),
                List.of(new GrowthResult.RecentExpChange(
                        10L, 100, 80, 20, 2, 3,
                        170, 250, OCCURRED_AT, "QUEST_COMPLETION", 2640L
                ))
        );
    }

    private UsernamePasswordAuthenticationToken playerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(264L, 26401L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(264L, null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
