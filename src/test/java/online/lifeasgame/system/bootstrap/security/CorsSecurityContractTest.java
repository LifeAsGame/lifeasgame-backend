package online.lifeasgame.system.bootstrap.security;

import io.jsonwebtoken.Claims;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
@DisplayName("Web CORS and security contract")
class CorsSecurityContractTest {

    private static final String PROTECTED_PATH = "/api/v1/security-contract/ping";
    private static final String LOCAL_ORIGIN = "http://localhost:3000";
    private static final String PRODUCTION_ORIGIN = "https://app.example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Nested
    @DisplayName("Configured origin flow")
    class ConfiguredOriginFlow {

        @Test
        @DisplayName("localhost:3000 preflight is allowed with credentials")
        void allowsLocalhostPreflight() throws Exception {
            preflight(LOCAL_ORIGIN, HttpMethod.GET, "Authorization")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }

        @Test
        @DisplayName("a second configured production origin is allowed")
        void allowsMultipleConfiguredOrigins() throws Exception {
            preflight(PRODUCTION_ORIGIN, HttpMethod.POST, "Content-Type")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PRODUCTION_ORIGIN));
        }

        @Test
        @DisplayName("preflight advertises the required methods and headers")
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
    }

    @Nested
    @DisplayName("Rejected origin flow")
    class RejectedOriginFlow {

        @Test
        @DisplayName("an unknown origin is rejected")
        void rejectsUnknownOrigin() throws Exception {
            preflight("https://unknown.example.com", HttpMethod.GET, "Authorization")
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @ParameterizedTest
        @ValueSource(strings = {"*", "https://*.example.com"})
        @DisplayName("credentialed policy rejects wildcard origins")
        void rejectsWildcardOrigins(String origin) {
            assertThatThrownBy(() -> new WebCorsProperties(List.of(origin)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("without wildcards");
        }
    }

    @Nested
    @DisplayName("Protected API flow")
    class ProtectedApiFlow {

        @Test
        @DisplayName("a configured preflight reaches CORS without JWT")
        void allowsProtectedPreflightWithoutJwt() throws Exception {
            preflight(LOCAL_ORIGIN, HttpMethod.GET, "Authorization")
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("the actual protected request still requires JWT")
        void rejectsActualRequestWithoutJwt() throws Exception {
            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_ORIGIN))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }

        @Test
        @DisplayName("a valid JWT keeps the protected API and Location header available")
        void allowsActualRequestWithJwtAndExposesLocation() throws Exception {
            Claims claims = mock(Claims.class);
            given(jwtProvider.parse("valid-access-token")).willReturn(Optional.of(claims));
            given(claims.getSubject()).willReturn("1");
            given(claims.get("pid", Long.class)).willReturn(2L);

            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                            containsString(HttpHeaders.LOCATION)
                    ))
                    .andExpect(header().string(HttpHeaders.LOCATION, PROTECTED_PATH));
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
        ResponseEntity<Void> ping() {
            return ResponseEntity.ok()
                    .location(URI.create(PROTECTED_PATH))
                    .build();
        }
    }
}
