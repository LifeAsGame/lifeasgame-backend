package online.lifeasgame.economy.infra;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import online.lifeasgame.economy.application.port.PaymentGateway;
import online.lifeasgame.economy.domain.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("검증이 구현되지 않은 결제 어댑터")
class TossPaymentGatewayTest {

    @ParameterizedTest
    @MethodSource("unverifiedCharges")
    @DisplayName("유효해 보이는 값과 경계 입력을 모두 거절한다")
    void rejectsUnverifiedCharge(String paymentKey, String orderId, long amount, Currency currency) {
        assertThat(new TossPaymentGateway().confirmCharge(paymentKey, orderId, amount, currency))
                .isFalse();
    }

    static Stream<Arguments> unverifiedCharges() {
        Stream<Arguments> positive = Stream.of(Currency.GOLD, Currency.GEM)
                .flatMap(currency -> Stream.of(1L, 100L, Long.MAX_VALUE)
                        .map(amount -> Arguments.of("synthetic-payment", "synthetic-order", amount, currency)));
        return Stream.concat(positive, Stream.of(
                Arguments.of(null, "synthetic-order", 1L, Currency.GOLD),
                Arguments.of("", "synthetic-order", 1L, Currency.GOLD),
                Arguments.of(" ", "synthetic-order", 1L, Currency.GOLD),
                Arguments.of("synthetic-payment", null, 1L, Currency.GEM),
                Arguments.of("synthetic-payment", "", 1L, Currency.GEM),
                Arguments.of("synthetic-payment", " ", 1L, Currency.GEM),
                Arguments.of("synthetic-payment", "synthetic-order", 0L, Currency.GOLD),
                Arguments.of("synthetic-payment", "synthetic-order", -1L, Currency.GEM),
                Arguments.of("synthetic-payment", "synthetic-order", Long.MIN_VALUE, Currency.GOLD),
                Arguments.of("synthetic-payment", "synthetic-order", 1L, null)
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "test", "local", "prod"})
    @DisplayName("기본 및 각 프로필에서 실제 rejecting Component 하나를 선택한다")
    void registersRejectingComponent(String profile) {
        try (var context = new AnnotationConfigApplicationContext()) {
            if (!profile.isEmpty()) {
                context.getEnvironment().setActiveProfiles(profile);
            }
            var scanner = new ClassPathBeanDefinitionScanner(context);
            var gatewayFilter = new AssignableTypeFilter(PaymentGateway.class);
            scanner.addExcludeFilter((reader, factory) -> !gatewayFilter.match(reader, factory));
            scanner.scan("online.lifeasgame");
            context.refresh();

            assertThat(context.getBeansOfType(PaymentGateway.class).values())
                    .singleElement().isExactlyInstanceOf(TossPaymentGateway.class);
            assertThat(context.getBean(PaymentGateway.class)
                    .confirmCharge("synthetic-payment", "synthetic-order", 100L, Currency.GOLD))
                    .isFalse();
        }
    }

    @Test
    @DisplayName("결제 키 원문을 로그에 기록하지 않는다")
    void doesNotLogPaymentKey() {
        Logger logger = (Logger) LoggerFactory.getLogger(TossPaymentGateway.class);
        Level previousLevel = logger.getLevel();
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            new TossPaymentGateway().confirmCharge("synthetic-private-marker", "synthetic-order", 100L, Currency.GOLD);
            assertThat(appender.list).noneSatisfy(event ->
                    assertThat(event.getFormattedMessage()).contains("synthetic-private-marker"));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
