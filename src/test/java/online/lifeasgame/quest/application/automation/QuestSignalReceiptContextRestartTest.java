package online.lifeasgame.quest.application.automation;

import online.lifeasgame.LifeasgameApplication;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.trigger.LifeLogRecordedQuestTrigger;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("QuestSignalReceipt ApplicationContext 재시작")
class QuestSignalReceiptContextRestartTest {

    private static final Long PLAYER_ID = 195002L;
    private static final Long LEGACY_PLAYER_ID = 195003L;
    private static final Long CONTENT_PLAYER_ID = 215002L;

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

        @Test
        @DisplayName("Legacy AUTO_CREATE/null Receipt도 재시작 후 REPLAYED다")
        void replaysLegacyFingerprintFromDurableReceipt()
                throws Exception {
            QuestSignal signal = signal(
                    LEGACY_PLAYER_ID,
                    "source:collection:legacy-context-restart"
            );
            QuestSignalProcessingResult applied;
            String currentFingerprint;
            String legacyFingerprint;

            try (ConfigurableApplicationContext first = startContext()) {
                QuestSignalFingerprint fingerprint = first.getBean(
                        QuestSignalFingerprint.class
                );
                currentFingerprint = fingerprint.fingerprint(signal);
                legacyFingerprint = fingerprint.legacyFingerprint(signal);
                applied = first.getBean(
                                QuestSignalProcessingService.class
                        )
                        .process(signal);
            }

            assertThat(receiptFingerprint(LEGACY_PLAYER_ID))
                    .isEqualTo(currentFingerprint)
                    .isNotEqualTo(legacyFingerprint);
            replaceReceiptFingerprint(
                    LEGACY_PLAYER_ID,
                    legacyFingerprint
            );

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
            assertThat(receiptCount(LEGACY_PLAYER_ID)).isEqualTo(1);
            assertThat(acceptanceCount(LEGACY_PLAYER_ID)).isEqualTo(1);
            assertThat(progressValue(LEGACY_PLAYER_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("LifeLog global id Receipt도 새 Context에서 REPLAYED를 반환한다")
        void replaysLifeLogSignalFromDurableReceipt() throws Exception {
            QuestSignal signal;
            QuestSignalProcessingResult applied;

            try (ConfigurableApplicationContext first = startContext()) {
                Instant acceptedAt = first.getBean(QuestService.class)
                        .accept(
                                CONTENT_PLAYER_ID,
                                new QuestCommand.Accept(
                                        QuestCode.Q_RECORD_THREE_TRACES.value(),
                                        null,
                                        null
                                )
                        )
                        .acceptedAt();
                LifeLogRecorded event = new LifeLogRecorded(
                        "215-context-restart-a",
                        LifeLogRecorded.EVENT_TYPE,
                        LifeLogRecorded.EVENT_VERSION,
                        acceptedAt.plusSeconds(1),
                        CONTENT_PLAYER_ID,
                        215200L,
                        1,
                        LifeLogSubtype.STUDY,
                        LifeLogEntryMode.FULL,
                        null,
                        null,
                        null,
                        null
                );
                signal = first.getBean(LifeLogRecordedQuestTrigger.class)
                        .translate(event)
                        .stream()
                        .filter(candidate -> candidate.questCode()
                                == QuestCode.Q_RECORD_THREE_TRACES)
                        .findFirst()
                        .orElseThrow();
                applied = first.getBean(
                                QuestSignalProcessingService.class
                        )
                        .process(signal);
            }

            QuestSignalProcessingResult replayed;
            try (ConfigurableApplicationContext restarted = startContext()) {
                replayed = restarted.getBean(
                                QuestSignalProcessingService.class
                        )
                        .process(signal);
            }

            assertThat(applied.outcome())
                    .isEqualTo(QuestSignalProcessingResult.Outcome.APPLIED);
            assertThat(replayed.outcome())
                    .isEqualTo(QuestSignalProcessingResult.Outcome.REPLAYED);
            assertThat(replayed.receiptId()).isEqualTo(applied.receiptId());
            assertThat(receiptCount(CONTENT_PLAYER_ID)).isEqualTo(1);
            assertThat(acceptanceCount(CONTENT_PLAYER_ID)).isEqualTo(1);
            assertThat(progressValue(CONTENT_PLAYER_ID)).isEqualTo(1);
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
        return signal(
                PLAYER_ID,
                "source:collection:context-restart"
        );
    }

    private QuestSignal signal(Long playerId, String correlationId) {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        playerId,
                        1
                )
                .occurredAt(Instant.parse("2026-07-24T03:00:00Z"))
                .correlationId(correlationId)
                .attribute("category", "BOOK")
                .build();
    }

    private int receiptCount() throws Exception {
        return receiptCount(PLAYER_ID);
    }

    private int receiptCount(Long playerId) throws Exception {
        return count(
                "SELECT COUNT(*) FROM quest_signal_receipts "
                        + "WHERE player_id = " + playerId
        );
    }

    private int acceptanceCount() throws Exception {
        return acceptanceCount(PLAYER_ID);
    }

    private int acceptanceCount(Long playerId) throws Exception {
        return count(
                "SELECT COUNT(*) FROM quest_acceptances "
                        + "WHERE player_id = " + playerId
        );
    }

    private int progressValue() throws Exception {
        return progressValue(PLAYER_ID);
    }

    private int progressValue(Long playerId) throws Exception {
        return count(
                "SELECT progress_value FROM quest_acceptances "
                        + "WHERE player_id = " + playerId
        );
    }

    private String receiptFingerprint(Long playerId) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ); PreparedStatement statement = connection.prepareStatement(
                "SELECT payload_fingerprint FROM quest_signal_receipts "
                        + "WHERE player_id = ?"
        )) {
            statement.setLong(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                String fingerprint = resultSet.getString(1);
                assertThat(resultSet.next()).isFalse();
                return fingerprint;
            }
        }
    }

    private void replaceReceiptFingerprint(
            Long playerId,
            String fingerprint
    ) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ); PreparedStatement statement = connection.prepareStatement(
                "UPDATE quest_signal_receipts "
                        + "SET payload_fingerprint = ? "
                        + "WHERE player_id = ?"
        )) {
            statement.setString(1, fingerprint);
            statement.setLong(2, playerId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
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
