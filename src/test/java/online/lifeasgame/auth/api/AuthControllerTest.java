package online.lifeasgame.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.auth.api.request.AuthRequest;
import online.lifeasgame.auth.application.AuthFacade;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.support.ControllerSliceTest;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AuthFacade authFacade;

    @MockitoBean AppErrorProperties appErrorProperties;
    @MockitoBean ErrorDocLinker errorDocLinker;

    private static final AuthResult.TokenPair PAIR =
            new AuthResult.TokenPair("access-token","refresh-token",1L,2L);

    @Nested @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test @DisplayName("정상 → 200 + TokenPair")
        void success() throws Exception {
            when(authFacade.login(any(),any())).thenReturn(PAIR);
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Login("t@t.com","password1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.result.playerId").value(2));
        }

        @Test @DisplayName("이메일 형식 오류 → 400")
        void invalidEmail() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Login("not-email","password1"))))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("잘못된 자격증명 → 401")
        void badCredentials() throws Exception {
            when(authFacade.login(any(),any()))
                    .thenThrow(new AuthException(AuthError.BAD_CREDENTIALS));
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Login("t@t.com","wrongpass1"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test @DisplayName("정상 → 200 + requiresVerification=false")
        void success() throws Exception {
            when(authFacade.register(any(),any(),any()))
                    .thenReturn(AuthResult.RegisterResult.verified(PAIR));
            mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Register("t@t.com","password1","Kirito"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.requiresVerification").value(false))
                    .andExpect(jsonPath("$.result.tokenPair.accessToken").value("access-token"));
        }

        @Test @DisplayName("닉네임 1자 → 400")
        void shortNickname() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Register("t@t.com","password1","K"))))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("비밀번호 7자 → 400")
        void shortPassword() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Register("t@t.com","pass123","Kirito"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test @DisplayName("정상 → 200 + 새 TokenPair")
        void success() throws Exception {
            when(authFacade.refresh("rt"))
                    .thenReturn(new AuthResult.TokenPair("new","new-r",1L,2L));
            mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Refresh("rt"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.accessToken").value("new"));
        }

        @Test @DisplayName("유효하지 않은 토큰 → 401")
        void invalid() throws Exception {
            when(authFacade.refresh(any()))
                    .thenThrow(new AuthException(AuthError.TOKEN_INVALID));
            mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AuthRequest.Refresh("bad"))))
                    .andExpect(status().isUnauthorized());
        }
    }
}
