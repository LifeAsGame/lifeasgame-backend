package online.lifeasgame.character.api.player;

import online.lifeasgame.character.application.PlayerAchievementService;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerAchievementController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Player Achievement API contract")
class PlayerAchievementApiContractTest {

    private static final Instant ACQUIRED_AT =
            Instant.parse("2026-08-12T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerAchievementService playerAchievementService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("현재 Player의 획득 업적을 조회할 때")
    class ReadAchievements {

        @Test
        @DisplayName("playerId 없이 현재 Player endpoint를 노출한다")
        void exposesCurrentPlayerEndpoint() throws Exception {
            assertThat(PlayerAchievementController.class
                    .getDeclaredMethod("playerAchievementInfos")
                    .getParameterCount()).isZero();
        }

        @Test
        @DisplayName("기존 infos 항목의 여섯 필드를 그대로 반환한다")
        void preservesResponseShape() throws Exception {
            given(playerAchievementService.getPlayerAchievementInfos())
                    .willReturn(List.of(new PlayerAchievementResult.Info(
                            260L,
                            "HOME_FIRST",
                            "첫 Home",
                            "STORY",
                            "Home feed 업적",
                            ACQUIRED_AT
                    )));

            mockMvc.perform(get("/api/v1/players/achievements")
                            .with(authentication(playerAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.infos[0].achievementId")
                            .value(260))
                    .andExpect(jsonPath("$.result.infos[0].code")
                            .value("HOME_FIRST"))
                    .andExpect(jsonPath("$.result.infos[0].name")
                            .value("첫 Home"))
                    .andExpect(jsonPath("$.result.infos[0].category")
                            .value("STORY"))
                    .andExpect(jsonPath("$.result.infos[0].descMd")
                            .value("Home feed 업적"))
                    .andExpect(jsonPath("$.result.infos[0].acquiredAt")
                            .value("2026-08-12T00:00:00Z"));
        }

        @Test
        @DisplayName("인증이 없으면 401이다")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/players/achievements"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private UsernamePasswordAuthenticationToken playerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(260L, 26001L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
