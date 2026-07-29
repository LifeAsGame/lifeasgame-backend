package online.lifeasgame.lifelog.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.application.OutboxRelayResult;
import online.lifeasgame.platform.outbox.application.OutboxRelayScheduler;
import online.lifeasgame.platform.outbox.application.OutboxRelayService;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import({
        LifeLogRecordedOutboxIntegrationTest.ClockConfiguration.class,
        LifeLogRecordedOutboxIntegrationTest.ListenerConfiguration.class
})
@DisplayName("LifeLogRecorded MySQL Outbox")
class LifeLogRecordedOutboxIntegrationTest {

    private static final String ALIAS = "lifelog.recorded.v1";
    private static final Long PLAYER_ID = 199L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T11:00:00.123456Z");
    private static final Set<String> PAYLOAD_FIELDS = Set.of(
            "eventId",
            "eventType",
            "eventVersion",
            "occurredAt",
            "playerId",
            "lifeLogId",
            "sourceDefinitionVersion",
            "subtype",
            "entryMode",
            "reflectionScope",
            "periodKey",
            "primaryRoleId"
    );
    private static final List<String> FORBIDDEN_FIELDS = List.of(
            "title",
            "originalTitle",
            "memo",
            "conditionNote",
            "acquiredFrom",
            "tags",
            "photo",
            "body",
            "review",
            "conversationText",
            "category",
            "duration",
            "calories",
            "episode"
    );

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_lifelog_recorded")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired
    private CollectionLogService collectionLogService;

    @Autowired
    private ExerciseLogService exerciseLogService;

    @Autowired
    private MediaLogService mediaLogService;

    @Autowired
    private OutboxRelayService relayService;

    @Autowired
    private OutboxEventCodecRegistry codecRegistry;

    @Autowired
    private OutboxProperties outboxProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private LifeLogRecordedProbe probe;

    @Autowired
    private ApplicationContext applicationContext;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM collection_log_tags");
        jdbcTemplate.update("DELETE FROM media_log_tags");
        jdbcTemplate.update("DELETE FROM life_log_records");
        jdbcTemplate.update("DELETE FROM collection_logs");
        jdbcTemplate.update("DELETE FROM exercise_logs");
        jdbcTemplate.update("DELETE FROM media_logs");
        jdbcTemplate.update("DELETE FROM outbox_events");
        outboxProperties.setBatchSize(50);
        outboxProperties.setMaxAttempts(3);
        outboxProperties.setRetryDelayMs(0);
        outboxProperties.setLeaseDurationMs(30_000);
        outboxProperties.setInstanceId("lifelog-recorded-integration");
        probe.reset();
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("Collection create commit은 Source와 대표 Fact를 한 건씩 저장한다")
    void commitsCollectionAndFact() {
        Long sourceId = collectionLogService.create(
                PLAYER_ID,
                collectionCommand()
        ).id();

        assertThat(count("collection_logs")).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(countByAlias()).isEqualTo(1);
        assertRecorded(sourceId, LifeLogType.COLLECTION);
        assertThat(countByAlias("lifelog.collection-logged.v1"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Exercise create commit은 Source와 대표 Fact를 한 건씩 저장한다")
    void commitsExerciseAndFact() {
        Long sourceId = exerciseLogService.create(
                PLAYER_ID,
                exerciseCommand()
        ).id();

        assertThat(count("exercise_logs")).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(countByAlias()).isEqualTo(1);
        assertRecorded(sourceId, LifeLogType.EXERCISE);
        assertThat(countByAlias("lifelog.exercise-logged.v1"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Media create commit은 Source와 대표 Fact를 한 건씩 저장한다")
    void commitsMediaAndFact() {
        Long sourceId = mediaLogService.create(
                PLAYER_ID,
                mediaCommand()
        ).id();

        assertThat(count("media_logs")).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(countByAlias()).isEqualTo(1);
        assertRecorded(sourceId, LifeLogType.MEDIA);
        assertThat(count("outbox_events")).isEqualTo(1);
    }

    @Test
    @DisplayName("강제 rollback은 Source와 대표 Fact를 모두 제거한다")
    void rollsBackSourceAndFactTogether() {
        transactionTemplate.executeWithoutResult(status -> {
            collectionLogService.create(PLAYER_ID, collectionCommand());
            status.setRollbackOnly();
        });

        assertThat(count("collection_logs")).isZero();
        assertThat(count("life_log_records")).isZero();
        assertThat(countByAlias()).isZero();
        assertThat(count("outbox_events")).isZero();
    }

    @Test
    @DisplayName("대표 Fact payload는 계약 필드만 포함하고 private subtype 값을 제외한다")
    void keepsPayloadMinimal() throws Exception {
        collectionLogService.create(PLAYER_ID, collectionCommand());
        exerciseLogService.create(PLAYER_ID, exerciseCommand());
        mediaLogService.create(PLAYER_ID, mediaCommand());

        List<String> payloads = recordedPayloads();

        assertThat(payloads).hasSize(3);
        for (String payload : payloads) {
            JsonNode json = objectMapper.readTree(payload);
            Set<String> fields = new LinkedHashSet<>();
            json.fieldNames().forEachRemaining(fields::add);
            assertThat(fields).containsExactlyInAnyOrderElementsOf(
                    PAYLOAD_FIELDS
            );
            assertThat(json.get("eventType").asText())
                    .isEqualTo("LifeLogRecorded");
            assertThat(json.get("eventVersion").asInt()).isEqualTo(1);
            assertThat(json.get("primaryRoleId").isNull()).isTrue();
            assertThat(payload)
                    .doesNotContain(LifeLogRecorded.class.getName())
                    .doesNotContain("Private collection title")
                    .doesNotContain("Private original title")
                    .doesNotContain("Private condition")
                    .doesNotContain("Private source")
                    .doesNotContain("Private exercise memo")
                    .doesNotContain("Private media title")
                    .doesNotContain("private-tag");
            assertThat(FORBIDDEN_FIELDS).allSatisfy(
                    field -> assertThat(json.has(field)).isFalse()
            );
        }
    }

    @Test
    @DisplayName("Relay는 Codec으로 복원한 동일 LifeLogRecorded를 Listener에 전달한다")
    void relaysDecodedEvent() {
        mediaLogService.create(PLAYER_ID, mediaCommand());
        LifeLogRecorded expected = onlyRecordedEvent();

        OutboxRelayResult result = relayService.relayBatch();

        assertThat(result.claimed()).isEqualTo(1);
        assertThat(result.published()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(probe.events()).containsExactly(expected);
    }

    @Test
    @DisplayName("Relay는 저장돼 있던 legacy v1 JSON도 decode해 전달한다")
    void relaysLegacyPayload() {
        mediaLogService.create(PLAYER_ID, mediaCommand());
        String eventId = "21300000-0000-0000-0000-000000000001";
        String payload = """
                {
                  "eventId": "%s",
                  "eventVersion": 1,
                  "playerId": 199,
                  "lifeLogId": 51,
                  "lifeLogType": "EXERCISE",
                  "primaryRoleId": null,
                  "occurredAt": "2026-07-24T11:00:00.123456Z"
                }
                """.formatted(eventId);
        int updated = jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET payload = ?
                WHERE event_type = ?
                """,
                payload,
                ALIAS
        );
        assertThat(updated).isEqualTo(1);
        LifeLogRecorded decoded = (LifeLogRecorded) codecRegistry.decode(
                ALIAS,
                payload
        );
        assertThat(decoded.legacyLifeLogType())
                .isEqualTo(LifeLogType.EXERCISE);

        OutboxRelayResult result = relayService.relayBatch();

        assertThat(result.published())
                .as("relay=%s, lastError=%s", result, lastError())
                .isEqualTo(1);
        assertThat(probe.events()).singleElement().satisfies(event -> {
            assertThat(event.legacyLifeLogType())
                    .isEqualTo(LifeLogType.EXERCISE);
            assertThat(event.isContentReady()).isFalse();
        });
    }

    @Test
    @DisplayName("통합 테스트에서는 Outbox Scheduler를 명시적으로 비활성화한다")
    void disablesScheduler() {
        assertThat(
                applicationContext.getBeansOfType(
                        OutboxRelayScheduler.class
                )
        ).isEmpty();
    }

    private CollectionCommand.Create collectionCommand() {
        return new CollectionCommand.Create(
                "BOOK",
                "Private collection title",
                "Private original title",
                2,
                "Private condition",
                "Private source",
                Set.of("private-tag")
        );
    }

    private ExerciseCommand.Create exerciseCommand() {
        return new ExerciseCommand.Create(
                "RUNNING",
                30,
                5.0,
                250,
                LocalDate.of(2026, 7, 24),
                "Private exercise memo",
                new LifeLogRecordMetadataCommand(
                        "ACTIVITY",
                        null
                )
        );
    }

    private MediaLogCommand.Create mediaCommand() {
        return new MediaLogCommand.Create(
                "MOVIE",
                "Private media title",
                "Private original title",
                0,
                1,
                "PLANNED",
                Set.of("private-tag"),
                new LifeLogRecordMetadataCommand("STUDY", null)
        );
    }

    private void assertRecorded(Long sourceId, LifeLogType type) {
        LifeLogRecorded event = onlyRecordedEvent();
        Long headerId = headerId(type, sourceId);
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventType()).isEqualTo("LifeLogRecorded");
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.playerId()).isEqualTo(PLAYER_ID);
        assertThat(event.lifeLogId()).isEqualTo(headerId);
        assertThat(event.sourceDefinitionVersion()).isEqualTo(1);
        assertThat(event.entryMode()).isEqualTo(LifeLogEntryMode.FULL);
        assertThat(event.legacyLifeLogType()).isNull();
        if (type == LifeLogType.COLLECTION) {
            assertThat(event.subtype()).isNull();
            assertThat(event.isContentReady()).isFalse();
        } else {
            assertThat(event.subtype()).isIn(
                    LifeLogSubtype.ACTIVITY,
                    LifeLogSubtype.STUDY
            );
            assertThat(event.isContentReady()).isTrue();
        }
        assertThat(event.primaryRoleId()).isNull();
        assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    private Long headerId(LifeLogType type, Long sourceId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM life_log_records
                WHERE source_type = ?
                  AND source_id = ?
                """,
                Long.class,
                type.name(),
                sourceId
        );
    }

    private LifeLogRecorded onlyRecordedEvent() {
        List<LifeLogRecorded> events = jdbcTemplate.query(
                """
                SELECT event_type, payload
                FROM outbox_events
                WHERE event_type = ?
                """,
                (resultSet, rowNum) -> {
                    DomainEvent decoded = codecRegistry.decode(
                            resultSet.getString("event_type"),
                            resultSet.getString("payload")
                    );
                    return (LifeLogRecorded) decoded;
                },
                ALIAS
        );
        assertThat(events).hasSize(1);
        return events.getFirst();
    }

    private List<String> recordedPayloads() {
        return jdbcTemplate.queryForList(
                """
                SELECT payload
                FROM outbox_events
                WHERE event_type = ?
                ORDER BY id
                """,
                String.class,
                ALIAS
        );
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Integer.class
        );
    }

    private int countByAlias() {
        return countByAlias(ALIAS);
    }

    private int countByAlias(String alias) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_events
                WHERE event_type = ?
                """,
                Integer.class,
                alias
        );
    }

    private String lastError() {
        return jdbcTemplate.queryForObject(
                "SELECT last_error FROM outbox_events",
                String.class
        );
    }

    @TestConfiguration
    static class ClockConfiguration {

        @Bean
        @Primary
        Clock lifeLogRecordedTestClock() {
            return Clock.fixed(OCCURRED_AT, ZoneOffset.UTC);
        }
    }

    @TestConfiguration
    static class ListenerConfiguration {

        @Bean
        LifeLogRecordedProbe lifeLogRecordedProbe() {
            return new LifeLogRecordedProbe();
        }
    }

    static class LifeLogRecordedProbe {

        private final List<LifeLogRecorded> events =
                new CopyOnWriteArrayList<>();

        @EventListener
        public void on(LifeLogRecorded event) {
            events.add(event);
        }

        List<LifeLogRecorded> events() {
            return List.copyOf(events);
        }

        void reset() {
            events.clear();
        }
    }
}
