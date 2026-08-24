package online.lifeasgame.system.bootstrap.security;

import io.jsonwebtoken.Claims;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CorsSecurityContractTest.ContractController.class,
        properties = "lifeasgame.web.cors.allowed-origins="
                + "http://localhost:3000,https://app.example.com"
)
@ActiveProfiles("test")
@Import({
        CorsSecurityContractTest.ContractController.class,
        SecurityConfig.class,
        WebMvcTestConfig.class
})
@DisplayName("CORS 요청을 처리할 때")
class CorsSecurityContractTest {

    private static final String PROTECTED_PATH = "/api/v1/security-contract/ping";
    private static final String LOCAL_ORIGIN = "http://localhost:3000";
    private static final String PRODUCTION_ORIGIN = "https://app.example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserAuthApi userAuthApi;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("허용된 Origin이면")
    class AllowedOrigin {

        @Test
        @DisplayName("localhost:3000의 protected preflight를 credentials와 함께 허용한다")
        void allowsLocalhostPreflight() throws Exception {
            preflight(LOCAL_ORIGIN, HttpMethod.GET, "Authorization")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }

        @Test
        @DisplayName("쉼표로 설정한 두 번째 production Origin도 허용한다")
        void allowsMultipleConfiguredOrigins() throws Exception {
            preflight(PRODUCTION_ORIGIN, HttpMethod.POST, "Content-Type")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PRODUCTION_ORIGIN));
        }

        @Test
        @DisplayName("preflight에 필요한 method와 header만 알린다")
        void advertisesRequiredMethodsAndHeaders() throws Exception {
            preflight(PRODUCTION_ORIGIN, HttpMethod.PATCH,
                    "Authorization", "Content-Type", "Accept")
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                            allOf(
                                    containsString("GET"),
                                    containsString("POST"),
                                    containsString("PUT"),
                                    containsString("PATCH"),
                                    containsString("DELETE"),
                                    containsString("OPTIONS")
                            )
                    ))
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                            containsString("Authorization")
                    ))
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                            containsString("Content-Type")
                    ))
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                            containsString("Accept")
                    ));
        }

        @Test
        @DisplayName("actual protected request는 계속 JWT를 요구한다")
        void rejectsActualRequestWithoutJwt() throws Exception {
            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }

        @Test
        @DisplayName("유효한 JWT의 actual protected request를 허용하고 불필요한 header는 expose하지 않는다")
        void allowsActualRequestWithJwt() throws Exception {
            Claims claims = mock(Claims.class);
            given(jwtProvider.parseAccessToken("valid-access-token"))
                    .willReturn(Optional.of(claims));
            given(claims.getSubject()).willReturn("1");
            given(claims.get("pid", Long.class)).willReturn(2L);
            given(userAuthApi.resolveAuthorization(1L)).willReturn(
                    Optional.of(new UserAuthApi.AccountAuthorization(
                            true,
                            false
                    ))
            );

            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                            PRODUCTION_ORIGIN
                    ))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
        }
    }

    @Nested
    @DisplayName("허용되지 않은 Origin이면")
    class UnknownOrigin {

        @Test
        @DisplayName("preflight를 permissive header 없이 거부한다")
        void rejectsUnknownOriginPreflight() throws Exception {
            preflight("https://unknown.example.com", HttpMethod.GET, "Authorization")
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        @DisplayName("actual request도 permissive header 없이 거부한다")
        void rejectsUnknownOriginActualRequest() throws Exception {
            mockMvc.perform(get(PROTECTED_PATH)
                            .with(user("user").roles("USER"))
                            .header(HttpHeaders.ORIGIN, "https://unknown.example.com"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Nested
    @DisplayName("허용되지 않은 method/header이면")
    class UnapprovedRequestContract {

        @Test
        @DisplayName("TRACE preflight를 403으로 거부한다")
        void rejectsTraceMethod() throws Exception {
            preflight(PRODUCTION_ORIGIN, HttpMethod.TRACE, "Authorization")
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
        }

        @Test
        @DisplayName("X-Unapproved-Header preflight를 403으로 거부한다")
        void rejectsUnapprovedHeader() throws Exception {
            preflight(PRODUCTION_ORIGIN, HttpMethod.GET, "X-Unapproved-Header")
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
        }
    }

    @Nested
    @DisplayName("Origin 설정 계약을 검증할 때")
    class OriginConfiguration {

        @ParameterizedTest
        @ValueSource(strings = {
                "http://localhost:3000",
                "https://app.example.com",
                "https://app.example.com:8443"
        })
        @DisplayName("exact HTTP Origin은 허용한다")
        void acceptsExactOrigins(String origin) {
            assertThat(new WebCorsProperties(List.of(origin)).allowedOrigins())
                    .containsExactly(origin);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "*",
                "https://*.example.com",
                "app.example.com",
                "ftp://app.example.com",
                "https://app.example.com/",
                "https://app.example.com/api",
                "https://app.example.com?tenant=1",
                "https://app.example.com#fragment",
                "https://exa mple.com"
        })
        @DisplayName("exact HTTP Origin이 아니면 startup에서 거부한다")
        void rejectsInvalidOrigins(String origin) {
            assertThatThrownBy(() -> new WebCorsProperties(List.of(origin)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exact http(s)://host[:port]");
        }

        @Test
        @DisplayName("Origin 목록이 없거나 비어 있으면 startup에서 거부한다")
        void rejectsMissingOrigins() {
            assertThatThrownBy(() -> new WebCorsProperties(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new WebCorsProperties(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private ResultActions preflight(String origin, HttpMethod method, String... requestedHeaders)
            throws Exception {
        return mockMvc.perform(options(PROTECTED_PATH)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, String.join(",", requestedHeaders)));
    }

    @RestController
    static class ContractController {

        @GetMapping(PROTECTED_PATH)
        String ping() {
            return "ok";
        }
    }
}
