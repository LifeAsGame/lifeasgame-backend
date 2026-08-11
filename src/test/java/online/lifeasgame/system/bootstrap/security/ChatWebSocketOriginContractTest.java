package online.lifeasgame.system.bootstrap.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "lifeasgame.web.cors.allowed-origins=http://localhost:3000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Realtime origin을 검증할 때")
class ChatWebSocketOriginContractTest {

    private static final String LOCAL_ORIGIN = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("SockJS /ws Origin을 확인하면")
    class SockJsOrigin {

        @Test
        @DisplayName("설정된 Origin의 인증된 info 요청을 허용한다")
        void allowsConfiguredOrigin() throws Exception {
            mockMvc.perform(get("/ws/info")
                            .with(user("user").roles("USER"))
                            .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                            LOCAL_ORIGIN
                    ));
        }

        @Test
        @DisplayName("설정된 Origin이어도 인증되지 않은 info 요청은 거부한다")
        void rejectsAnonymousRequest() throws Exception {
            mockMvc.perform(get("/ws/info")
                            .header(HttpHeaders.ORIGIN, LOCAL_ORIGIN))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                            LOCAL_ORIGIN
                    ));
        }

        @Test
        @DisplayName("설정되지 않은 Origin의 인증된 info 요청을 거부한다")
        void rejectsUnknownOrigin() throws Exception {
            mockMvc.perform(get("/ws/info")
                            .with(user("user").roles("USER"))
                            .header(HttpHeaders.ORIGIN, "https://unknown.example.com"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
