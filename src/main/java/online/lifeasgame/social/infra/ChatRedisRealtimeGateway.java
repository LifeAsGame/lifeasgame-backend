package online.lifeasgame.social.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.ChatRealtimeGateway;
import online.lifeasgame.social.application.ChatRealtimePayload;
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
        try {
            redisTemplate.convertAndSend(
                    topicResolver.topic(payload.channelId()),
                    objectMapper.writeValueAsString(payload)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize chat payload", e);
        }
    }
}
