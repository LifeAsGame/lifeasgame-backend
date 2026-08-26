package online.lifeasgame.character.api.admin;

import online.lifeasgame.character.api.admin.request.AdminPlayerHolderGrantRequest;
import online.lifeasgame.character.application.AdminPlayerHolderGrantService;
import online.lifeasgame.character.application.PlayerAchievementService;
import online.lifeasgame.character.application.PlayerHolderQueryService;
import online.lifeasgame.character.application.PlayerTitleService;
import online.lifeasgame.character.application.command.AdminPlayerHolderGrantCommand;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.platform.security.jwt.JwtProperties;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AdminPlayerAchievementController.class,
        AdminPlayerTitleController.class
})
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminPlayerHolderGrantControllerTest.JwtTestConfig.class
})
@DisplayName("Admin Player holder grant controller contract")
class AdminPlayerHolderGrantControllerTest {

    private static final long PLAYER_ID = 316L;
    private static final long ACHIEVEMENT_ID = 3161L;
    private static final long TITLE_ID = 3162L;
    private static final long USER_ID = 31601L;
    private static final long ADMIN_ID = 31602L;
    private static final String SECRET =
            "admin-holder-grant-test-secret-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private AdminPlayerHolderGrantService holderGrantService;

    @MockitoBean
    private PlayerAchievementService playerAchievementService;

    @MockitoBean
    private PlayerTitleService playerTitleService;

    @MockitoBean
    private PlayerHolderQueryService playerHolderQueryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @BeforeEach
    void setUp() {
        Instant acquiredAt = Instant.parse("2026-08-26T00:00:00Z");
        given(holderGrantService.grantAchievement(any())).willReturn(
                new PlayerAchievementResult.Granted(
                        PLAYER_ID,
                        ACHIEVEMENT_ID,
                        "ACH-316",
                        "Achievement 316",
                        "STORY",
                        acquiredAt
                )
        );
        given(holderGrantService.grantTitle(any())).willReturn(
                new PlayerTitleResult.Created(
                        PLAYER_ID,
                        TITLE_ID,
                        "TITLE-316",
                        "Title 316",
                        "SPECIAL",
                        acquiredAt
                )
        );
    }

    @Test
    @DisplayName("두 body는 reason 하나만 노출한다")
    void exposesReasonOnlyBody() {
        assertThat(componentNames(AdminPlayerHolderGrantRequest.Grant.class))
                .containsExactly("reason");
    }

    @Test
    @DisplayName("ADMIN 요청은 두 holder grant에 safe header metadata를 전달한다")
    void allowsAdminGrants() throws Exception {
        allowAdmin();

        mockMvc.perform(grantRequest(achievementPath())
                        .header("Idempotency-Key", "achievement-316")
                        .header("X-Correlation-Id", "request-achievement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.achievementId")
                        .value(ACHIEVEMENT_ID));
        mockMvc.perform(grantRequest(titlePath())
                        .header("Idempotency-Key", "title-316")
                        .header("X-Correlation-Id", "request-title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.titleId").value(TITLE_ID));

        var achievement = org.mockito.ArgumentCaptor.forClass(
                AdminPlayerHolderGrantCommand.GrantAchievement.class
        );
        var title = org.mockito.ArgumentCaptor.forClass(
                AdminPlayerHolderGrantCommand.GrantTitle.class
        );
        verify(holderGrantService).grantAchievement(achievement.capture());
        verify(holderGrantService).grantTitle(title.capture());
        assertThat(achievement.getValue()).satisfies(command -> {
            assertThat(command.playerId()).isEqualTo(PLAYER_ID);
            assertThat(command.achievementId()).isEqualTo(ACHIEVEMENT_ID);
            assertThat(command.reason()).isEqualTo("CASE-316");
            assertThat(command.idempotencyKey()).isEqualTo("achievement-316");
            assertThat(command.correlationId()).isEqualTo("request-achievement");
        });
        assertThat(title.getValue()).satisfies(command -> {
            assertThat(command.playerId()).isEqualTo(PLAYER_ID);
            assertThat(command.titleId()).isEqualTo(TITLE_ID);
            assertThat(command.reason()).isEqualTo("CASE-316");
            assertThat(command.idempotencyKey()).isEqualTo("title-316");
            assertThat(command.correlationId()).isEqualTo("request-title");
        });
    }

    @Test
    @DisplayName("두 command 모두 Idempotency-Key가 없으면 400이다")
    void requiresIdempotencyKey() throws Exception {
        allowAdmin();

        mockMvc.perform(grantRequest(achievementPath()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(grantRequest(titlePath()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(holderGrantService);
    }

    @Test
    @DisplayName("unsafe reason과 header 및 non-positive path를 400으로 거부한다")
    void rejectsUnsafeInput() throws Exception {
        allowAdmin();

        mockMvc.perform(grantRequest(achievementPath())
                        .header("Idempotency-Key", "unsafe key"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(titlePath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "title-316")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CASE-316\u202Eprivate")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(grantRequest(titlePath())
                        .header("Idempotency-Key", "title-316")
                        .header("X-Correlation-Id", "unsafe correlation"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(grantRequest(
                        "/admin/v1/players/0/achievements/3161"
                ).header("Idempotency-Key", "achievement-316"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(holderGrantService);
    }

    @Test
    @DisplayName("persisted USER authority는 두 grant를 403으로 거부한다")
    void rejectsUser() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false))
        );

        mockMvc.perform(grantRequest(achievementPath(), USER_ID)
                        .header("Idempotency-Key", "achievement-user"))
                .andExpect(status().isForbidden());
        mockMvc.perform(grantRequest(titlePath(), USER_ID)
                        .header("Idempotency-Key", "title-user"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(holderGrantService);
    }

    @Test
    @DisplayName("/api/v1/admin alias는 존재하지 않는다")
    void hasNoAlias() throws Exception {
        allowAdmin();

        mockMvc.perform(grantRequest(
                        "/api/v1/admin/players/316/achievements/3161"
                ).header("Idempotency-Key", "achievement-316"))
                .andExpect(status().isNotFound());
    }

    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    grantRequest(String path) {
        return grantRequest(path, ADMIN_ID);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    grantRequest(String path, long userId) {
        return post(path)
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("CASE-316"));
    }

    private String achievementPath() {
        return "/admin/v1/players/316/achievements/3161";
    }

    private String titlePath() {
        return "/admin/v1/players/316/titles/3162";
    }

    private String body(String reason) {
        return """
                {"reason": "%s"}
                """.formatted(reason);
    }

    private void allowAdmin() {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );
    }

    private String bearer(long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId, null);
    }

    private static JwtProvider provider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiryMs(3_600_000L);
        properties.setRefreshTokenExpiryMs(604_800_000L);
        return new JwtProvider(properties);
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtProvider jwtProvider() {
            return provider();
        }
    }
}
