package online.lifeasgame.quest.application.automation;

import online.lifeasgame.LifeasgameApplication;
import online.lifeasgame.quest.domain.QuestCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("QuestSignalReceipt ApplicationContext 재시작")
class QuestSignalReceiptContextRestartTest {

    private static final Long PLAYER_ID = 195002L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_signal_restart")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    @Nested
    @DisplayName("최초 적용 후 Redis 상태가 없는 새 Context를 기동할 때")
    class ReplayAfterRestart {

        @Test
        @DisplayName("MySQL Receipt를 기준으로 REPLAYED를 반환한다")
        void replaysFromDurableReceipt() throws Exception {
            QuestSignal signal = signal();
            QuestSignalProcessingResult applied;

            try (ConfigurableApplicationContext first = startContext()) {
                applied = first.getBean(QuestSignalProcessingService.class)
                        .process(signal);
                assertThat(applied.outcome())
                        .isEqualTo(
                                QuestSignalProcessingResult.Outcome.APPLIED
                        );
            }

            QuestSignalProcessingResult replayed;
            try (ConfigurableApplicationContext restarted = startContext()) {
                replayed = restarted.getBean(
                                QuestSignalProcessingService.class
                        )
                        .process(signal);
            }

            assertThat(replayed.outcome())
                    .isEqualTo(QuestSignalProcessingResult.Outcome.REPLAYED);
            assertThat(replayed.receiptId()).isEqualTo(applied.receiptId());
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(acceptanceCount()).isEqualTo(1);
            assertThat(progressValue()).isEqualTo(1);
        }
    }

    private ConfigurableApplicationContext startContext() {
        return new SpringApplicationBuilder(
                LifeasgameApplication.class,
                RestartTestConfig.class
        )
                .web(WebApplicationType.SERVLET)
                .registerShutdownHook(false)
                .run(
                        "--spring.profiles.active=test,migration-test",
                        "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                        "--spring.datasource.username=" + MYSQL.getUsername(),
                        "--spring.datasource.password=" + MYSQL.getPassword(),
                        "--spring.datasource.driver-class-name="
                                + MYSQL.getDriverClassName(),
                        "--server.port=0",
                        "--spring.main.banner-mode=off"
                );
    }

    private QuestSignal signal() {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        PLAYER_ID,
                        1
                )
                .occurredAt(Instant.parse("2026-07-24T03:00:00Z"))
                .correlationId("source:collection:context-restart")
                .attribute("category", "BOOK")
                .build();
    }

    private int receiptCount() throws Exception {
        return count(
                "SELECT COUNT(*) FROM quest_signal_receipts "
                        + "WHERE player_id = " + PLAYER_ID
        );
    }

    private int acceptanceCount() throws Exception {
        return count(
                "SELECT COUNT(*) FROM quest_acceptances "
                        + "WHERE player_id = " + PLAYER_ID
        );
    }

    private int progressValue() throws Exception {
        return count(
                "SELECT progress_value FROM quest_acceptances "
                        + "WHERE player_id = " + PLAYER_ID
        );
    }

    private int count(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            int value = resultSet.getInt(1);
            assertThat(resultSet.next()).isFalse();
            return value;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RestartTestConfig {

        @Bean
        @Primary
        QuestProgressStore restartQuestProgressStore() {
            return new QuestProgressStore() {
                @Override
                public int increment(
                        QuestCode questCode,
                        Long playerId,
                        int delta,
                        Duration ttl
                ) {
                    return delta;
                }

                @Override
                public int set(
                        QuestCode questCode,
                        Long playerId,
                        int value,
                        Duration ttl
                ) {
                    return value;
                }

                @Override
                public void reset(QuestCode questCode, Long playerId) {
                }
            };
        }
    }
}
