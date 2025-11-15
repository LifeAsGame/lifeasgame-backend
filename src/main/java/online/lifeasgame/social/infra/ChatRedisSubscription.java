package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.realtime.RedisPubSubSubscription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(ChatRedisMessageSubscriber.class)
public class ChatRedisSubscription implements RedisPubSubSubscription {

    private final ChatRedisMessageSubscriber subscriber;
    private final ChatRealtimeTopicResolver topicResolver;

    @Override
    public PatternTopic topic() {
        return new PatternTopic(topicResolver.pattern());
    }

    @Override
    public MessageListener listener() {
        return subscriber;
    }
}
