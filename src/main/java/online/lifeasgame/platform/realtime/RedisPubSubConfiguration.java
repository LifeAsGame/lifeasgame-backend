package online.lifeasgame.platform.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisPubSubConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSubConfiguration.class);

    @Bean
    @ConditionalOnBean(RedisPubSubSubscription.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ObjectProvider<RedisPubSubSubscription> subscriptions
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        SimpleAsyncTaskExecutor listenerExecutor = new SimpleAsyncTaskExecutor("redis-pubsub-listener-");
        listenerExecutor.setConcurrencyLimit(10);
        container.setTaskExecutor(listenerExecutor);
        SimpleAsyncTaskExecutor subscriptionExecutor = new SimpleAsyncTaskExecutor("redis-pubsub-subscription-");
        subscriptionExecutor.setConcurrencyLimit(10);
        container.setSubscriptionExecutor(subscriptionExecutor);
        container.setErrorHandler(error -> log.error("Redis pub/sub listener error", error));
        subscriptions.orderedStream()
                .forEach(subscription -> container.addMessageListener(subscription.listener(), subscription.topic()));
        return container;
    }
}
