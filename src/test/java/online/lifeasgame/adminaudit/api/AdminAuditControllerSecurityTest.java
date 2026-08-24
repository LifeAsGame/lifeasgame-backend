package online.lifeasgame.adminaudit.api;

import online.lifeasgame.adminaudit.application.AdminAuditQueryService;
import online.lifeasgame.adminaudit.application.result.AdminAuditQueryResult;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditController.class)
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminAuditControllerSecurityTest.JwtTestConfig.class
})
@DisplayName("Admin Audit controller security")
class AdminAuditControllerSecurityTest {

    private static final long USER_ID = 30401L;
    private static final long ADMIN_ID = 30402L;
    private static final String SECRET =
            "admin-audit-test-secret-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private AdminAuditQueryService queryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @BeforeEach
    void setUp() {
        given(queryService.list(
                null, null, null, null, null,
                null, null, null, null, 50
        )).willReturn(new AdminAuditQueryResult.Page(
                List.of(new AdminAuditQueryResult.Item(
                        1L,
                        ADMIN_ID,
                        "USER_STATUS_CHANGE",
                        "USER",
                        "42",
                        "CASE-304",
                        "SUCCESS",
                        "request-304",
                        null,
                        Instant.parse("2026-08-24T03:04:05Z")
                )),
                null
        ));
    }

    @Test
    @DisplayName("persisted USER authority는 403이다")
    void rejectsUser() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, false))
        );

        mockMvc.perform(get("/admin/v1/audit-events")
                        .header("Authorization", bearer(USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("persisted ADMIN authority는 safe audit page를 조회한다")
    void allowsAdmin() throws Exception {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );

        mockMvc.perform(get("/admin/v1/audit-events")
                        .header("Authorization", bearer(ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].actorUserId")
                        .value(ADMIN_ID))
                .andExpect(jsonPath("$.result.items[0].action")
                        .value("USER_STATUS_CHANGE"))
                .andExpect(jsonPath("$.result.items[0].targetId")
                        .value("42"))
                .andExpect(jsonPath("$.result.items[0].correlationId")
                        .value("request-304"));
    }

    @Test
    @DisplayName("/api/v1/admin alias는 존재하지 않는다")
    void hasNoApiV1AdminAlias() throws Exception {
        given(userAuthApi.resolveAuthorization(ADMIN_ID)).willReturn(
                Optional.of(new UserAuthApi.AccountAuthorization(true, true))
        );

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .header("Authorization", bearer(ADMIN_ID)))
                .andExpect(status().isNotFound());
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
