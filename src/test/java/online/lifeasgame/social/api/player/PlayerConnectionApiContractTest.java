package online.lifeasgame.social.api.player;

import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.social.application.ConnectionQueryService;
import online.lifeasgame.social.application.result.ConnectionResult;
import online.lifeasgame.support.WebMvcTestConfig;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.system.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerConnectionController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Current Player Connections API contract")
class PlayerConnectionApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectionQueryService connectionQueryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("두 canonical route는 directional privacy를 지킨 page envelope를 반환한다")
    void exposesCanonicalConnectionRoutes() throws Exception {
        ConnectionResult.Peer peer = new ConnectionResult.Peer(
                284L,
                "Peer",
                "MAGE",
                7
        );
        given(connectionQueryService.followings(0, 20)).willReturn(
                ConnectionResult.Page.of(
                        List.of(new ConnectionResult.Following(282L, peer, true, false)),
                        0,
                        20,
                        1
                )
        );
        given(connectionQueryService.followers(0, 20)).willReturn(
                ConnectionResult.Page.of(
                        List.of(new ConnectionResult.Follower(peer, true, 283L)),
                        0,
                        20,
                        1
                )
        );

        mockMvc.perform(get("/api/v1/connections/followings")
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.*", hasSize(5)))
                .andExpect(jsonPath("$.result.contents[0].*", hasSize(4)))
                .andExpect(jsonPath("$.result.contents[0].followId").value(282))
                .andExpect(jsonPath("$.result.contents[0].peer.playerId").value(284))
                .andExpect(jsonPath("$.result.contents[0].peer.name").value("Peer"))
                .andExpect(jsonPath("$.result.contents[0].peer.job").value("MAGE"))
                .andExpect(jsonPath("$.result.contents[0].peer.level").value(7))
                .andExpect(jsonPath("$.result.contents[0].muted").value(true))
                .andExpect(jsonPath("$.result.contents[0].blocked").value(false));

        mockMvc.perform(get("/api/v1/connections/followers")
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.*", hasSize(5)))
                .andExpect(jsonPath("$.result.contents[0].*", hasSize(3)))
                .andExpect(jsonPath("$.result.contents[0].peer.playerId").value(284))
                .andExpect(jsonPath("$.result.contents[0].followedBack").value(true))
                .andExpect(jsonPath("$.result.contents[0].outboundFollowId").value(283))
                .andExpect(jsonPath("$.result.contents[0].followId").doesNotExist())
                .andExpect(jsonPath("$.result.contents[0].muted").doesNotExist())
                .andExpect(jsonPath("$.result.contents[0].blocked").doesNotExist());
    }

    private UsernamePasswordAuthenticationToken playerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(284L, 28401L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
