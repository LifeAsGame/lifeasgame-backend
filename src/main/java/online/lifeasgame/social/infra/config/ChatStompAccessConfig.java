package online.lifeasgame.social.infra.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.platform.security.jwt.JwtPrincipal;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.social.application.ChatReader;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@RequiredArgsConstructor
public class ChatStompAccessConfig implements WebSocketMessageBrokerConfigurer {
    private static final Pattern TOPIC = Pattern.compile("/topic/social/chat/([1-9][0-9]*)");
    private static final Pattern SEND = Pattern.compile("/app/social/chat/([1-9][0-9]*)/send");
    private final JwtProvider jwtProvider;
    private final UserAuthApi userAuthApi;
    private final PlayerLookupApi playerLookupApi;
    private final ChatReader chatReader;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ExecutorChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor headers = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (headers == null || headers.getCommand() == null) return message;
                if (headers.getCommand() == StompCommand.CONNECT) {
                    String bearer = headers.getFirstNativeHeader("Authorization");
                    if (bearer == null || !bearer.startsWith("Bearer ")) throw denied();
                    String token = bearer.substring(7);
                    UsernamePasswordAuthenticationToken auth = authenticate(token);
                    headers.setUser(auth);
                    if (headers.getSessionId() == null) throw denied();
                    sessions.put(headers.getSessionId(), new Session(token, (JwtPrincipal) auth.getPrincipal()));
                    return message;
                }
                if (headers.getCommand() == StompCommand.DISCONNECT) {
                    sessions.remove(headers.getSessionId());
                    return message;
                }
                UsernamePasswordAuthenticationToken auth = authenticate(sessionToken(headers));
                if (!auth.getPrincipal().equals(sessions.get(headers.getSessionId()).principal())) throw denied();
                if (headers.getCommand() == StompCommand.SUBSCRIBE || headers.getCommand() == StompCommand.SEND) {
                    Matcher matcher = (headers.getCommand() == StompCommand.SUBSCRIBE ? TOPIC : SEND)
                            .matcher(headers.getDestination() == null ? "" : headers.getDestination());
                    if (!matcher.matches()) throw denied();
                    chatReader.getMemberChannel(Long.valueOf(matcher.group(1)), ((JwtPrincipal) auth.getPrincipal()).playerId());
                }
                return message;
            }

            @Override
            public Message<?> beforeHandle(Message<?> message, MessageChannel channel, org.springframework.messaging.MessageHandler handler) {
                StompHeaderAccessor headers = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (headers != null && headers.getCommand() != null
                        && headers.getCommand() != StompCommand.DISCONNECT) {
                    SecurityContextHolder.getContext().setAuthentication(authenticate(sessionToken(headers)));
                }
                return message;
            }

            @Override
            public void afterMessageHandled(Message<?> message, MessageChannel channel,
                                            org.springframework.messaging.MessageHandler handler, Exception ex) {
                SecurityContextHolder.clearContext();
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
                if (headers.getMessageType() != SimpMessageType.MESSAGE) return message;
                try {
                    UsernamePasswordAuthenticationToken auth = authenticate(sessionToken(headers));
                    if (!auth.getPrincipal().equals(sessions.get(headers.getSessionId()).principal())) return null;
                    Matcher matcher = TOPIC.matcher(headers.getDestination() == null ? "" : headers.getDestination());
                    if (!matcher.matches()) return null;
                    chatReader.getMemberChannel(Long.valueOf(matcher.group(1)), ((JwtPrincipal) auth.getPrincipal()).playerId());
                    return message;
                } catch (RuntimeException ex) {
                    return null;
                }
            }
        });
    }

    private String sessionToken(SimpMessageHeaderAccessor headers) {
        Session session = sessions.get(headers.getSessionId());
        if (session == null) throw denied();
        return session.token();
    }

    private UsernamePasswordAuthenticationToken authenticate(String token) {
        Claims claims = jwtProvider.parseAccessToken(token).orElseThrow(ChatStompAccessConfig::denied);
        try {
            Long userId = Long.valueOf(claims.getSubject());
            Long playerId = claims.get("pid", Long.class);
            if (playerId == null || !playerId.equals(playerLookupApi.findPlayerIdByUserId(userId))
                    || userAuthApi.resolveAuthorization(userId).filter(UserAuthApi.AccountAuthorization::active).isEmpty()) {
                throw denied();
            }
            return new UsernamePasswordAuthenticationToken(new JwtPrincipal(userId, playerId), null, List.of());
        } catch (IllegalArgumentException ex) {
            throw denied();
        }
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Chat STOMP access denied");
    }

    private record Session(String token, JwtPrincipal principal) {}
}
