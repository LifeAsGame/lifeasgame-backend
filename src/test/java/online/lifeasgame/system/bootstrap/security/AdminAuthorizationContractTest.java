package online.lifeasgame.system.bootstrap.security;

import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAuthorizationContractTest.ContractController.class)
@Import({
        AdminAuthorizationContractTest.ContractController.class,
        SecurityConfig.class,
        WebMvcTestConfig.class
})
@DisplayName("Admin authorization contract")
class AdminAuthorizationContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("인증 없이 Admin path에 접근하면 401이다")
    void rejectsUnauthenticatedAdminRequest() throws Exception {
        mockMvc.perform(get("/admin/v1/security-contract/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_USER는 Admin path에 접근할 수 없다")
    void rejectsUserRoleFromAdminPath() throws Exception {
        mockMvc.perform(get("/admin/v1/security-contract/ping")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN은 Admin matcher를 통과한다")
    void allowsAdminRoleThroughAdminMatcher() throws Exception {
        mockMvc.perform(get("/admin/v1/security-contract/ping")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_USER는 일반 authenticated path를 사용할 수 있다")
    void keepsAuthenticatedPlayerPathAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/security-contract/ping")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());
    }

    @RestController
    static class ContractController {

        @GetMapping("/admin/v1/security-contract/ping")
        String adminPing() {
            return "ok";
        }

        @GetMapping("/api/v1/security-contract/ping")
        String playerPing() {
            return "ok";
        }
    }
}
