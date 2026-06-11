package online.lifeasgame.platform.realtime;

import org.springframework.core.Ordered;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;

public interface RedisPubSubSubscription extends Ordered {
    PatternTopic topic();

    MessageListener listener();

    @Override
    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
