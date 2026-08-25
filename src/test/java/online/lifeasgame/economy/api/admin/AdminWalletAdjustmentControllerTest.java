package online.lifeasgame.economy.api.admin;

import online.lifeasgame.economy.application.AdminWalletAdjustmentService;
import online.lifeasgame.economy.application.ShopService;
import online.lifeasgame.economy.application.command.AdminWalletAdjustmentCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEconomyController.class)
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminWalletAdjustmentControllerTest.JwtTestConfig.class
})
@DisplayName("Admin Wallet adjustment controller contract")
class AdminWalletAdjustmentControllerTest {

    private static final long USER_ID = 30601L;
    private static final long ADMIN_ID = 30602L;
    private static final String SECRET =
            "admin-wallet-test-secret-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private ShopService shopService;

    @MockitoBean
    private AdminWalletAdjustmentService adjustmentService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @BeforeEach
    void setUp() {
        given(adjustmentService.adjust(any())).willReturn(
                new EconomyResult.WalletBalance(130L, "GOLD")
        );
    }

    @Test
    @DisplayName("persisted USER authority는 403이다")
    void rejectsUser() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false))
        );

        mockMvc.perform(request(USER_ID)
                        .header("Idempotency-Key", "wallet-adjust-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN의 valid metadata를 hardened use case에 전달한다")
    void allowsAdmin() throws Exception {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );

        mockMvc.perform(request(ADMIN_ID)
                        .header("Idempotency-Key", "wallet-adjust-306")
                        .header("X-Correlation-Id", "request-306"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(130))
                .andExpect(jsonPath("$.result.currency").value("GOLD"));

        org.mockito.ArgumentCaptor<AdminWalletAdjustmentCommand> command =
                org.mockito.ArgumentCaptor.forClass(
                        AdminWalletAdjustmentCommand.class
                );
        org.mockito.Mockito.verify(adjustmentService).adjust(command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue()).satisfies(value -> {
            org.assertj.core.api.Assertions.assertThat(value.playerId())
                    .isEqualTo(306L);
            org.assertj.core.api.Assertions.assertThat(value.reason())
                    .isEqualTo("CASE-306");
            org.assertj.core.api.Assertions.assertThat(value.idempotencyKey())
                    .isEqualTo("wallet-adjust-306");
            org.assertj.core.api.Assertions.assertThat(value.correlationId())
                    .isEqualTo("request-306");
        });
    }

    @Test
    @DisplayName("Admin command custom header의 browser preflight를 허용한다")
    void allowsCommandHeaderPreflight() throws Exception {
        mockMvc.perform(options("/admin/v1/economy/wallets/306/adjust")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "authorization, content-type, idempotency-key, x-correlation-id"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("idempotency-key")
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("x-correlation-id")
                ));
    }

    @Test
    @DisplayName("Idempotency-Key가 없으면 400이고 mutation에 진입하지 않는다")
    void requiresIdempotencyKey() throws Exception {
        allowAdmin();

        mockMvc.perform(request(ADMIN_ID))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adjustmentService);
    }

    @Test
    @DisplayName("unsafe idempotency key와 multiline reason은 400이다")
    void rejectsUnsafeMetadata() throws Exception {
        allowAdmin();

        mockMvc.perform(request(ADMIN_ID)
                        .header("Idempotency-Key", "unsafe key"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/admin/v1/economy/wallets/306/adjust")
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "wallet-adjust-306")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 30,
                                  "currency": "GOLD",
                                  "debit": false,
                                  "reason": "CASE-306\\nprivate"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adjustmentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u200B", "\u00A0", " \u200B \u00A0 "})
    @DisplayName("invisible-only reason은 400이고 mutation에 진입하지 않는다")
    void rejectsInvisibleReason(String reason) throws Exception {
        allowAdmin();

        mockMvc.perform(post("/admin/v1/economy/wallets/306/adjust")
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "wallet-adjust-306")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 30,
                                  "currency": "GOLD",
                                  "debit": false,
                                  "reason": "%s"
                                }
                                """.formatted(reason)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adjustmentService);
    }

    @Test
    @DisplayName("/api/v1/admin alias는 존재하지 않는다")
    void hasNoAlias() throws Exception {
        allowAdmin();

        mockMvc.perform(post("/api/v1/admin/economy/wallets/306/adjust")
                        .header("Authorization", bearer(ADMIN_ID))
                        .header("Idempotency-Key", "wallet-adjust-306")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    request(long userId) {
        return post("/admin/v1/economy/wallets/306/adjust")
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body());
    }

    private void allowAdmin() {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );
    }

    private String body() {
        return """
                {
                  "amount": 30,
                  "currency": "GOLD",
                  "debit": false,
                  "reason": "CASE-306"
                }
                """;
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
