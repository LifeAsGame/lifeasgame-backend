package online.lifeasgame.economy.infra.event;

import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("선택적 Economy Redis 이벤트 리스너")
class RedisEconomyEventPublisherIntegrationTest {

    private static final String CHANNEL = "test.economy.events";
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues("lifeasgame.economy.events.channel=" + CHANNEL);

    @Test
    @DisplayName("commit 전에는 발행하지 않고 commit 후 서로 다른 이벤트를 각각 한 번 발행한다")
    void publishesEachEventOnceAfterCommit() {
        runner.withPropertyValues("lifeasgame.economy.events.enabled=true").run(context -> {
            RedisTemplate<?, ?> redis = redisBoundary(context);
            EconomyEvent opened = event(EconomyEventType.LISTING_OPENED);
            EconomyEvent reserved = event(EconomyEventType.LISTING_RESERVED);

            context.getBean(TransactionTemplate.class).executeWithoutResult(status -> {
                context.publishEvent(opened);
                context.publishEvent(reserved);
                verifyNoInteractions(redis);
            });

            verify(redis).convertAndSend(CHANNEL, opened);
            verify(redis).convertAndSend(CHANNEL, reserved);
            verifyNoMoreInteractions(redis);
        });
    }

    @Test
    @DisplayName("rollback 또는 트랜잭션 없는 이벤트는 발행하지 않는다")
    void doesNotPublishWithoutCommit() {
        runner.withPropertyValues("lifeasgame.economy.events.enabled=true").run(context -> {
            RedisTemplate<?, ?> redis = redisBoundary(context);
            context.getBean(TransactionTemplate.class).executeWithoutResult(status -> {
                context.publishEvent(event(EconomyEventType.LISTING_OPENED));
                status.setRollbackOnly();
            });
            context.publishEvent(event(EconomyEventType.LISTING_RESERVED));
            verifyNoInteractions(redis);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("플래그가 false이거나 미설정이면 commit 후에도 발행하지 않는다")
    void doesNotPublishWhenDisabledOrUnset(boolean explicitlyDisabled) {
        ApplicationContextRunner disabled = explicitlyDisabled
                ? runner.withPropertyValues("lifeasgame.economy.events.enabled=false") : runner;
        disabled.run(context -> {
            RedisTemplate<?, ?> redis = redisBoundary(context);
            context.getBean(TransactionTemplate.class).executeWithoutResult(status ->
                    context.publishEvent(event(EconomyEventType.LISTING_OPENED)));
            verifyNoInteractions(redis);
        });
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("Redis 실패는 로그로 남기며 commit과 후속 이벤트 전달을 방해하지 않는다")
    void logsRedisFailureAndContinues(CapturedOutput output) {
        runner.withPropertyValues("lifeasgame.economy.events.enabled=true").run(context -> {
            RedisTemplate<?, ?> redis = redisBoundary(context);
            EconomyEvent opened = event(EconomyEventType.LISTING_OPENED);
            EconomyEvent reserved = event(EconomyEventType.LISTING_RESERVED);
            doThrow(new RedisConnectionFailureException("Redis unavailable"))
                    .when(redis).convertAndSend(CHANNEL, opened);

            assertThatCode(() -> context.getBean(TransactionTemplate.class).executeWithoutResult(status -> {
                context.publishEvent(opened);
                context.publishEvent(reserved);
            })).doesNotThrowAnyException();

            verify(redis).convertAndSend(CHANNEL, opened);
            verify(redis).convertAndSend(CHANNEL, reserved);
            verifyNoMoreInteractions(redis);
            assertThat(output).contains("Failed to publish economy event LISTING_OPENED");
        });
    }

    private static RedisTemplate<?, ?> redisBoundary(ApplicationContext context) {
        RedisTemplate<?, ?> redis = context.getBean(RedisTemplate.class);
        // Ignore Spring bean lifecycle callbacks before observing event publication.
        clearInvocations(redis);
        return redis;
    }

    private static EconomyEvent event(EconomyEventType type) {
        return EconomyEvent.builder(type).actorId(1L).listingId(2L).build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableConfigurationProperties(EconomyEventProperties.class)
    // Discover production listeners in both layers; replace only Redis wiring with a mock boundary.
    @ComponentScan(basePackages = {
            "online.lifeasgame.economy.application.event",
            "online.lifeasgame.economy.infra.event"
    }, excludeFilters = @ComponentScan.Filter(Configuration.class))
    static class Config {
        @Bean
        @SuppressWarnings("unchecked")
        RedisTemplate<String, EconomyEvent> economyEventRedisTemplate() {
            return mock(RedisTemplate.class);
        }

        @Bean
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        TransactionTemplate transactionTemplate(EmbeddedDatabase dataSource) {
            return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        }
    }
}
