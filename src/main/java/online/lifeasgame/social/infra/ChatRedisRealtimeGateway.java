package online.lifeasgame.social.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.ChatRealtimeGateway;
import online.lifeasgame.social.application.ChatRealtimePayload;
import online.lifeasgame.social.application.ChatReadRealtimePayload;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRedisRealtimeGateway implements ChatRealtimeGateway {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatRealtimeTopicResolver topicResolver;

    @Override
    public void publish(ChatRealtimePayload payload) {
        publishPayload(payload.channelId(), payload);
    }

    @Override
    public void publishRead(ChatReadRealtimePayload payload) {
        publishPayload(payload.channelId(), payload);
    }

    private void publishPayload(Long channelId, Object payload) {
        try {
            redisTemplate.convertAndSend(
                    topicResolver.topic(channelId),
                    objectMapper.writeValueAsString(payload)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize chat payload", e);
        }
    }
}
