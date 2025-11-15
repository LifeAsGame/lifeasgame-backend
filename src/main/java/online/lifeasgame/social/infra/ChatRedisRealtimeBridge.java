package online.lifeasgame.social.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.ChatRealtimePayload;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class ChatRedisRealtimeBridge {

    private final RedisMessageListenerContainer container;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate broker;
    private final ChatRealtimeTopicResolver topicResolver;

    @PostConstruct
    public void subscribe() {
        String pattern = topicResolver.pattern();
        container.addMessageListener(new Listener(), new PatternTopic(pattern));
    }

    class Listener implements MessageListener {
        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String json = new String(message.getBody());
                ChatRealtimePayload payload = objectMapper.readValue(json, ChatRealtimePayload.class);
                String destination = topicResolver.destination(payload.channelId());
                broker.convertAndSend(destination, payload);
            } catch (Exception e) {
                // logging
            }
        }
    }
}
