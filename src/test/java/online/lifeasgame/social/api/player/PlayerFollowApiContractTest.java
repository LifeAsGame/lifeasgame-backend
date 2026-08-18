package online.lifeasgame.social.api.player;

import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.social.application.FollowFacade;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerFollowController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Current Player Follow API contract")
class PlayerFollowApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowFacade followFacade;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("targetPlayerId가 null이면 Follow application 호출 전에 요청을 거부한다")
    void rejectsNullTargetBeforeApplication() throws Exception {
        mockMvc.perform(post("/api/v1/follows")
                        .with(authentication(playerAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetPlayerId\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(followFacade);
    }

    private UsernamePasswordAuthenticationToken playerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(287L, 28701L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
