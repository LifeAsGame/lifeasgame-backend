package online.lifeasgame.system.bootstrap.security;

import online.lifeasgame.platform.security.jwt.JwtProperties;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.user.api.admin.AdminUserController;
import online.lifeasgame.user.application.UserQueryService;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.internal.UserAuthApi;
import online.lifeasgame.user.application.result.UserResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminUserController.class,
        AdminAuthorizationContractTest.ContractController.class
})
@ActiveProfiles("test")
@Import({
        SecurityConfig.class,
        WebMvcTestConfig.class,
        AdminAuthorizationContractTest.ContractController.class,
        AdminAuthorizationContractTest.JwtTestConfig.class
})
@DisplayName("persisted Admin authorization contract")
class AdminAuthorizationContractTest {

    private static final long USER_ID = 30001L;
    private static final long ADMIN_ID = 30002L;
    private static final String SECRET =
            "admin-authority-test-secret-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @BeforeEach
    void setUp() {
        given(userQueryService.search(any())).willReturn(
                new UserResult.UserList(
                        List.of(),
                        new UserResult.UserList.PageInfo(0, 20, 0)
                )
        );
    }

    @Nested
    @DisplayName("GET /admin/v1/users")
    class AdminPath {

        @Test
        @DisplayName("token이 없으면 401이다")
        void rejectsUnauthenticatedRequest() throws Exception {
            mockMvc.perform(get("/admin/v1/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("persisted USER account이면 403이다")
        void rejectsUserAuthority() throws Exception {
            given(userAuthApi.resolveAuthorization(USER_ID))
                    .willReturn(Optional.of(activeUser()));

            mockMvc.perform(get("/admin/v1/users")
                            .header("Authorization", bearer(userToken())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("persisted ADMIN account이면 Player 없이도 controller에 진입한다")
        void allowsAdminAuthority() throws Exception {
            given(userAuthApi.resolveAuthorization(ADMIN_ID))
                    .willReturn(Optional.of(activeAdmin()));

            mockMvc.perform(get("/admin/v1/users")
                            .header("Authorization", bearer(adminToken())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("위조된 token은 401이다")
        void rejectsForgedToken() throws Exception {
            mockMvc.perform(get("/admin/v1/users")
                            .header("Authorization", bearer(
                                    provider(SECRET + "-forged", 3_600_000L)
                                            .createAccessToken(ADMIN_ID, null)
                            )))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("만료된 token은 401이다")
        void rejectsExpiredToken() throws Exception {
            mockMvc.perform(get("/admin/v1/users")
                            .header("Authorization", bearer(
                                    provider(SECRET, -1_000L)
                                            .createAccessToken(ADMIN_ID, null)
                            )))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("같은 token도 downgrade 다음 요청은 403, 계정 revocation 다음 요청은 401이다")
        void appliesDowngradeAndRevocationOnNextRequest() throws Exception {
            given(userAuthApi.resolveAuthorization(ADMIN_ID))
                    .willReturn(Optional.of(activeAdmin()))
                    .willReturn(Optional.of(activeUser()))
                    .willReturn(Optional.of(
                            new UserAuthApi.AccountAuthorization(false, false)
                    ));
            String token = adminToken();

            mockMvc.perform(get("/admin/v1/users")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/admin/v1/users")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/admin/v1/users")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("정상 USER access token은 일반 authenticated path를 유지한다")
    void keepsNormalUserAuthentication() throws Exception {
        given(userAuthApi.resolveAuthorization(USER_ID))
                .willReturn(Optional.of(activeUser()));

        mockMvc.perform(get("/api/v1/security-contract/ping")
                        .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk());
    }

    private UserAuthApi.AccountAuthorization activeUser() {
        return new UserAuthApi.AccountAuthorization(true, false);
    }

    private UserAuthApi.AccountAuthorization activeAdmin() {
        return new UserAuthApi.AccountAuthorization(true, true);
    }

    private String userToken() {
        return jwtProvider.createAccessToken(USER_ID, 300001L);
    }

    private String adminToken() {
        return jwtProvider.createAccessToken(ADMIN_ID, null);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private static JwtProvider provider(String secret, long accessExpiryMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setAccessTokenExpiryMs(accessExpiryMs);
        properties.setRefreshTokenExpiryMs(604_800_000L);
        return new JwtProvider(properties);
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtProvider jwtProvider() {
            return provider(SECRET, 3_600_000L);
        }
    }

    @RestController
    static class ContractController {

        @GetMapping("/api/v1/security-contract/ping")
        String playerPing() {
            return "ok";
        }
    }
}
