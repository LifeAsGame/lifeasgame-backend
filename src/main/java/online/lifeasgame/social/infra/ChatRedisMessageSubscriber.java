package online.lifeasgame.social.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.social.application.ChatRealtimePayload;
import online.lifeasgame.social.application.ChatReadRealtimePayload;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatRedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRealtimeTopicResolver topicResolver;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode node = objectMapper.readTree(message.getBody());
            Long channelId = node.path("channelId").asLong();
            Object payload = "READ_UPDATED".equals(node.path("eventType").asText())
                    ? objectMapper.treeToValue(node, ChatReadRealtimePayload.class)
                    : objectMapper.treeToValue(node, ChatRealtimePayload.class);
            messagingTemplate.convertAndSend(topicResolver.destination(channelId), payload);
            log.debug("Chat Redis forwarded eventType={} channelId={}", node.path("eventType").asText(), channelId);
        } catch (IOException e) {
            throw new IllegalStateException("failed to deserialize chat payload", e);
        }
    }
}
