package online.lifeasgame.social.infra.config;

import io.jsonwebtoken.Claims;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.social.application.ChatReader;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Chat STOMP 세션 권한")
class ChatStompAccessConfigTest {
    @Mock JwtProvider jwtProvider;
    @Mock UserAuthApi userAuthApi;
    @Mock PlayerLookupApi playerLookupApi;
    @Mock ChatReader chatReader;
    @Mock Claims claims;

    @Test
    @DisplayName("CONNECT 인증 후 채널 구독만 허용하고 권한 철회 후 전달을 막는다")
    void authorizesFramesAndDelivery() {
        given(jwtProvider.parseAccessToken("token")).willReturn(Optional.of(claims));
        given(claims.getSubject()).willReturn("21");
        given(claims.get("pid", Long.class)).willReturn(42L);
        given(playerLookupApi.findPlayerIdByUserId(21L)).willReturn(42L);
        given(userAuthApi.resolveAuthorization(21L))
                .willReturn(Optional.of(new UserAuthApi.AccountAuthorization(true, false)));
        var config = new ChatStompAccessConfig(jwtProvider, userAuthApi, playerLookupApi, chatReader);
        ChannelRegistration inboundRegistration = mock(ChannelRegistration.class);
        ChannelRegistration outboundRegistration = mock(ChannelRegistration.class);
        config.configureClientInboundChannel(inboundRegistration);
        config.configureClientOutboundChannel(outboundRegistration);
        var inboundCaptor = org.mockito.ArgumentCaptor.forClass(ChannelInterceptor.class);
        var outboundCaptor = org.mockito.ArgumentCaptor.forClass(ChannelInterceptor.class);
        verify(inboundRegistration).interceptors(inboundCaptor.capture());
        verify(outboundRegistration).interceptors(outboundCaptor.capture());
        ChannelInterceptor inbound = inboundCaptor.getValue();
        ChannelInterceptor outbound = outboundCaptor.getValue();

        Message<?> connect = frame(StompCommand.CONNECT, null);
        assertThat(inbound.preSend(connect, null)).isSameAs(connect);
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, "/topic/social/chat/17");
        assertThat(inbound.preSend(subscribe, null)).isSameAs(subscribe);
        verify(chatReader).getMemberChannel(17L, 42L);
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SEND, "/topic/social/chat/17"), null))
                .isInstanceOf(AccessDeniedException.class);
        SimpMessageHeaderAccessor deliveryHeaders = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        deliveryHeaders.setSessionId("session-1");
        deliveryHeaders.setDestination("/topic/social/chat/17");
        Message<?> delivered = MessageBuilder.createMessage(new byte[0], deliveryHeaders.getMessageHeaders());
        assertThat(outbound.preSend(delivered, null)).isSameAs(delivered);

        given(userAuthApi.resolveAuthorization(21L)).willReturn(Optional.empty());
        assertThat(outbound.preSend(delivered, null)).isNull();
    }

    private Message<?> frame(StompCommand command, String destination) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(command);
        headers.setSessionId("session-1");
        if (command == StompCommand.CONNECT) headers.setNativeHeader("Authorization", "Bearer token");
        if (destination != null) headers.setDestination(destination);
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
