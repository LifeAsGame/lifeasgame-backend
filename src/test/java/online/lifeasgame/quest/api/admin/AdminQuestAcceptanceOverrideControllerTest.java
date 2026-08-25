package online.lifeasgame.quest.api.admin;

import online.lifeasgame.platform.security.jwt.JwtProperties;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.application.AdminQuestAcceptanceOverrideService;
import online.lifeasgame.quest.application.QuestQueryService;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.command.AdminQuestAcceptanceOverrideCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.QuestTargetType;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQuestController.class)
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminQuestAcceptanceOverrideControllerTest.JwtTestConfig.class
})
@DisplayName("Admin Quest Acceptance override controller contract")
class AdminQuestAcceptanceOverrideControllerTest {

    private static final long ACCEPTANCE_ID = 308L;
    private static final long USER_ID = 30801L;
    private static final long ADMIN_ID = 30802L;
    private static final String SECRET =
            "admin-quest-test-secret-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private QuestService questService;

    @MockitoBean
    private QuestQueryService questQueryService;

    @MockitoBean
    private AdminQuestAcceptanceOverrideService overrideService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @BeforeEach
    void setUp() {
        given(overrideService.adjustProgress(any())).willReturn(
                acceptance(2, "IN_PROGRESS")
        );
        given(overrideService.changeStatus(any())).willReturn(
                acceptance(0, "CANCELED")
        );
    }

    @Test
    @DisplayName("progress body는 delta와 visible reason만 노출한다")
    void exposesOnlyRealProgressFields() {
        assertThat(Arrays.stream(
                        AdminQuestRequest.AdjustProgress.class
                                .getRecordComponents()
                ).map(component -> component.getName()))
                .containsExactly("delta", "reason");
        assertThat(Arrays.stream(
                        AdminQuestRequest.ChangeStatus.class
                                .getRecordComponents()
                ).map(component -> component.getName()))
                .containsExactly("status", "reason");
    }

    @Test
    @DisplayName("ADMIN progress 요청은 header metadata와 delta를 전달한다")
    void allowsProgress() throws Exception {
        allowAdmin();

        mockMvc.perform(progressRequest(ADMIN_ID)
                        .header("Idempotency-Key", "quest-progress-308")
                        .header("X-Correlation-Id", "request-progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.progressValue").value(2));

        var captor = org.mockito.ArgumentCaptor.forClass(
                AdminQuestAcceptanceOverrideCommand.AdjustProgress.class
        );
        verify(overrideService).adjustProgress(captor.capture());
        assertThat(captor.getValue()).satisfies(command -> {
            assertThat(command.acceptanceId()).isEqualTo(ACCEPTANCE_ID);
            assertThat(command.delta()).isEqualTo(2);
            assertThat(command.reason()).isEqualTo("CASE-308-PROGRESS");
            assertThat(command.idempotencyKey()).isEqualTo("quest-progress-308");
            assertThat(command.correlationId()).isEqualTo("request-progress");
        });
    }

    @Test
    @DisplayName("ADMIN status 요청은 header metadata와 transition을 전달한다")
    void allowsStatus() throws Exception {
        allowAdmin();

        mockMvc.perform(statusRequest(ADMIN_ID)
                        .header("Idempotency-Key", "quest-status-308")
                        .header("X-Correlation-Id", "request-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CANCELED"));

        var captor = org.mockito.ArgumentCaptor.forClass(
                AdminQuestAcceptanceOverrideCommand.ChangeStatus.class
        );
        verify(overrideService).changeStatus(captor.capture());
        assertThat(captor.getValue()).satisfies(command -> {
            assertThat(command.acceptanceId()).isEqualTo(ACCEPTANCE_ID);
            assertThat(command.status()).isEqualTo("CANCELED");
            assertThat(command.reason()).isEqualTo("CASE-308-STATUS");
            assertThat(command.idempotencyKey()).isEqualTo("quest-status-308");
            assertThat(command.correlationId()).isEqualTo("request-status");
        });
    }

    @Test
    @DisplayName("persisted USER authority는 override를 403으로 거부한다")
    void rejectsUser() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false))
        );

        mockMvc.perform(progressRequest(USER_ID)
                        .header("Idempotency-Key", "quest-progress-user"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(overrideService);
    }

    @Test
    @DisplayName("두 command 모두 Idempotency-Key가 없으면 400이다")
    void requiresIdempotencyKey() throws Exception {
        allowAdmin();

        mockMvc.perform(progressRequest(ADMIN_ID))
                .andExpect(status().isBadRequest());
        mockMvc.perform(statusRequest(ADMIN_ID))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(overrideService);
    }

    @Test
    @DisplayName("invalid delta와 unsafe reason/key/correlation은 400이다")
    void rejectsUnsafeInput() throws Exception {
        allowAdmin();

        mockMvc.perform(patch(progressPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "unsafe key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(-1, "CASE-308")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(statusPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "quest-status-308")
                        .header("X-Correlation-Id", "unsafe correlation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("CANCELED", "CASE-308")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(progressPath())
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "quest-progress-308")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(1, "CASE-308\\nprivate")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(overrideService);
    }

    @Test
    @DisplayName("/api/v1/admin alias는 존재하지 않는다")
    void hasNoAlias() throws Exception {
        allowAdmin();

        mockMvc.perform(patch(
                        "/api/v1/admin/quests/acceptances/308/progress"
                )
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "quest-progress-308")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(2, "CASE-308")))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    progressRequest(long userId) {
        return patch(progressPath())
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(progressBody(2, "CASE-308-PROGRESS"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    statusRequest(long userId) {
        return patch(statusPath())
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusBody("CANCELED", "CASE-308-STATUS"));
    }

    private String progressPath() {
        return "/admin/v1/quests/acceptances/308/progress";
    }

    private String statusPath() {
        return "/admin/v1/quests/acceptances/308/status";
    }

    private String progressBody(int delta, String reason) {
        return """
                {
                  "delta": %d,
                  "reason": "%s"
                }
                """.formatted(delta, reason);
    }

    private String statusBody(String value, String reason) {
        return """
                {
                  "status": "%s",
                  "reason": "%s"
                }
                """.formatted(value, reason);
    }

    private void allowAdmin() {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );
    }

    private String bearer(long userId) {
        return "Bearer " + jwtProvider.createAccessToken(userId, null);
    }

    private static QuestResult.Acceptance acceptance(
            int progress,
            String status
    ) {
        return new QuestResult.Acceptance(
                ACCEPTANCE_ID,
                1L,
                10L,
                "quest:test:admin-override",
                "Admin override",
                "MAIN",
                null,
                QuestTargetType.COUNT,
                10,
                progress,
                status,
                "USER_CONFIRM",
                "NONE",
                LocalDate.of(1970, 1, 1),
                LocalDate.of(9999, 12, 31),
                Instant.parse("2026-08-25T00:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
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
