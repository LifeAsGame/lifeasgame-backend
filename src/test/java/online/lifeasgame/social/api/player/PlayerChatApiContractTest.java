package online.lifeasgame.social.api.player;

import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.social.application.ChatService;
import online.lifeasgame.social.application.FriendChatQueryService;
import online.lifeasgame.social.application.result.ChatResult;
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

@WebMvcTest(controllers = PlayerChatController.class)
@Import({SecurityConfig.class, WebMvcTestConfig.class})
@DisplayName("Current Player Direct Friend Chat API contract")
class PlayerChatApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private FriendChatQueryService friendChatQueryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AppErrorProperties appErrorProperties;

    @MockitoBean
    private ErrorDocLinker errorDocLinker;

    @Test
    @DisplayName("canonical route는 peer를 포함한 전용 FriendChannel 목록을 반환한다")
    void returnsCanonicalFriendChannels() throws Exception {
        given(friendChatQueryService.friendChannels()).willReturn(List.of(
                new ChatResult.FriendChannel(
                        288L,
                        new ChatResult.Peer(289L, "Peer", "MAGE", 7),
                        true
                )
        ));

        mockMvc.perform(get("/api/v1/chat/channels/friends")
                        .with(authentication(playerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].*", hasSize(3)))
                .andExpect(jsonPath("$.result[0].channelId").value(288))
                .andExpect(jsonPath("$.result[0].peer.*", hasSize(4)))
                .andExpect(jsonPath("$.result[0].peer.playerId").value(289))
                .andExpect(jsonPath("$.result[0].peer.name").value("Peer"))
                .andExpect(jsonPath("$.result[0].peer.job").value("MAGE"))
                .andExpect(jsonPath("$.result[0].peer.level").value(7))
                .andExpect(jsonPath("$.result[0].readOnly").value(true))
                .andExpect(jsonPath("$.result[0].userId").doesNotExist())
                .andExpect(jsonPath("$.result[0].name").doesNotExist());
    }

    private UsernamePasswordAuthenticationToken playerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(288L, 28801L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
