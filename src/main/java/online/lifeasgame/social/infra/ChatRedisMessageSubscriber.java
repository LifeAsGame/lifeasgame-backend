package online.lifeasgame.social.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.ChatRealtimePayload;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@ConditionalOnBean({SimpMessagingTemplate.class, ChatRedisRealtimeGateway.class})
public class ChatRedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRealtimeTopicResolver topicResolver;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatRealtimePayload payload = objectMapper.readValue(message.getBody(), ChatRealtimePayload.class);
            messagingTemplate.convertAndSend(topicResolver.destination(payload.channelId()), payload);
        } catch (IOException e) {
            throw new IllegalStateException("failed to deserialize chat payload", e);
        }
    }
}
