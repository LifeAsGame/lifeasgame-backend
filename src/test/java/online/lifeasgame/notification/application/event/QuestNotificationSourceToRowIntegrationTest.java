package online.lifeasgame.notification.application.event;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.notification.domain.PlayerNotification;
import online.lifeasgame.platform.outbox.application.OutboxClaim;
import online.lifeasgame.platform.outbox.application.OutboxClaimService;
import online.lifeasgame.platform.outbox.application.OutboxCompletionService;
import online.lifeasgame.platform.outbox.application.OutboxDispatchAttempt;
import online.lifeasgame.platform.outbox.application.OutboxRelayResult;
import online.lifeasgame.platform.outbox.application.OutboxRelayService;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(QuestNotificationSourceToRowIntegrationTest.ClockConfiguration.class)
@DisplayName("Quest Notification source-to-row MySQL 통합")
class QuestNotificationSourceToRowIntegrationTest {

    private static final long PLAYER_ID = 325001L;
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-08-01T01:00:00Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-01T02:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_notification")
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
        registry.add("app.outbox.batch-size", () -> 50);
        registry.add("app.outbox.max-attempts", () -> 3);
        registry.add("app.outbox.retry-delay-ms", () -> 0);
        registry.add("app.outbox.instance-id", () -> "quest-notification-proof");
    }

    @Autowired
    private QuestService questService;

    @Autowired
    private DomainEventPublisher eventPublisher;

    @Autowired
    private OutboxRelayService relayService;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxDispatchAttempt dispatchAttempt;

    @Autowired
    private OutboxCompletionService completionService;

    @Autowired
    private OutboxEventCodecRegistry codecRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MutableClock clock;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        clock.set(ACCEPTED_AT);
        jdbcTemplate.update("DELETE FROM player_notifications");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM player_growth_changes");
        jdbcTemplate.update("DELETE FROM reward_settlement_lines");
        jdbcTemplate.update("DELETE FROM reward_settlements");
        jdbcTemplate.update(
                "DELETE FROM quest_signal_receipts WHERE player_id = ?",
                PLAYER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM player_quest_routes WHERE player_id = ?",
                PLAYER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM quest_acceptances WHERE player_id = ?",
                PLAYER_ID
        );
        jdbcTemplate.update("DELETE FROM player WHERE id = ?", PLAYER_ID);
        insertPlayer();
        selectRecordRoute();
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("실제 Quest 완료 source의 재전달도 완료 알림 한 건과 미진행 Route를 남긴다")
    void storesQuestCompletedOnceWithoutAdvancingRoute() {
        completeFirstTraceQuest();
        RouteState routeBeforeDispatch = routeState();

        List<OutboxClaim> questClaims = claimService.claimBatch();
        OutboxClaim completedClaim = completedClaim(questClaims);
        QuestEvent completedEvent = decodeQuestEvent(completedClaim);
        questClaims.forEach(claim -> {
            dispatchAttempt.dispatch(claim);
            if (claim.equals(completedClaim)) {
                dispatchAttempt.dispatch(claim);
            }
            completionService.complete(claim);
        });

        assertNotification(
                NotificationType.QUEST_COMPLETED,
                completedClaim.eventId(),
                completedEvent.occurredAt(),
                "퀘스트 완료",
                "퀘스트를 완료했습니다."
        );
        assertThat(routeState()).isEqualTo(routeBeforeDispatch);
    }

    @Test
    @DisplayName("실제 typed reward-ready Fact의 재전달도 보상 준비 알림 한 건만 남긴다")
    void storesQuestRewardReadyOnce() {
        completeFirstTraceQuest();
        relayQuestEventsOnce();

        List<OutboxClaim> rewardClaims = claimService.claimBatch();
        assertThat(rewardClaims).hasSize(1);
        OutboxClaim rewardClaim = rewardClaims.getFirst();
        assertThat(rewardClaim.eventType()).isEqualTo("quest.reward-ready.v1");
        QuestRewardReadyFact rewardFact =
                (QuestRewardReadyFact) codecRegistry.decode(
                        rewardClaim.eventType(),
                        rewardClaim.payload()
                );

        dispatchAttempt.dispatch(rewardClaim);
        dispatchAttempt.dispatch(rewardClaim);
        completionService.complete(rewardClaim);

        assertNotification(
                NotificationType.QUEST_REWARD_READY,
                rewardClaim.eventId(),
                rewardFact.occurredAt(),
                "퀘스트 보상 준비",
                "퀘스트 보상을 확인할 수 있습니다."
        );
    }

    private QuestResult.Acceptance completeFirstTraceQuest() {
        QuestResult.Acceptance accepted = questService.accept(
                PLAYER_ID,
                new QuestCommand.Accept(
                        QuestCode.Q_RECORD_FIRST_TRACE.value(),
                        null,
                        null
                )
        );
        append(new LifeLogRecorded(
                "325-life-log-recorded",
                LifeLogRecorded.EVENT_TYPE,
                LifeLogRecorded.EVENT_VERSION,
                ACCEPTED_AT.plusSeconds(1),
                PLAYER_ID,
                325101L,
                1,
                LifeLogSubtype.STUDY,
                LifeLogEntryMode.FULL,
                null,
                null,
                null,
                null
        ));
        clock.set(COMPLETED_AT);

        OutboxRelayResult sourceRelay = relayService.relayBatch();
        assertThat(sourceRelay.failed()).isZero();
        assertThat(sourceRelay.published()).isEqualTo(1);
        return accepted;
    }

    private void relayQuestEventsOnce() {
        OutboxRelayResult questRelay = relayService.relayBatch();
        assertThat(questRelay.failed()).isZero();
        assertThat(questRelay.published()).isEqualTo(3);
    }

    private void append(online.lifeasgame.core.event.DomainEvent event) {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publish(event)
        );
    }

    private OutboxClaim completedClaim(List<OutboxClaim> claims) {
        List<OutboxClaim> completed = claims.stream()
                .filter(claim -> claim.eventType().equals("quest.event.v1"))
                .filter(claim -> decodeQuestEvent(claim).type()
                        == QuestEventType.QUEST_COMPLETED)
                .toList();
        assertThat(completed).hasSize(1);
        return completed.getFirst();
    }

    private QuestEvent decodeQuestEvent(OutboxClaim claim) {
        return (QuestEvent) codecRegistry.decode(
                claim.eventType(),
                claim.payload()
        );
    }

    private void assertNotification(
            NotificationType type,
            String sourceEventId,
            Instant occurredAt,
            String title,
            String body
    ) {
        entityManager.clear();
        List<PlayerNotification> notifications = entityManager.createQuery(
                        """
                        SELECT notification
                        FROM PlayerNotification notification
                        WHERE notification.playerId = :playerId
                          AND notification.type = :type
                        """,
                        PlayerNotification.class
                )
                .setParameter("playerId", PLAYER_ID)
                .setParameter("type", type)
                .getResultList();

        assertThat(notifications).hasSize(1);
        PlayerNotification notification = notifications.getFirst();
        assertThat(notification.getPlayerId()).isEqualTo(PLAYER_ID);
        assertThat(notification.getSourceEventId()).isEqualTo(sourceEventId);
        assertThat(notification.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getBody()).isEqualTo(body);
    }

    private void insertPlayer() {
        jdbcTemplate.update("""
                INSERT INTO player (
                    id, user_id, name, gender, level, exp,
                    hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat,
                    extra_stats, status_effects, version, created_at, updated_at
                ) VALUES (
                    ?, ?, 'Quest Notification Tester', 'male', 1, 0,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, PLAYER_ID + 100000L);
    }

    private void selectRecordRoute() {
        jdbcTemplate.update("""
                INSERT INTO player_quest_routes (
                    player_id, route_id, current_step_id, status,
                    selected_at, completed_at, version, created_at, updated_at
                )
                SELECT ?, route.id, step.id, 'IN_PROGRESS',
                       CURRENT_TIMESTAMP(6), NULL, 0,
                       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                FROM quest_routes route
                JOIN quest_route_steps step ON step.route_id = route.id
                WHERE route.code = 'ROUTE_RECORD_START'
                  AND step.step_code = 'RS_RECORD_01_LEAVE_TRACE'
                """, PLAYER_ID);
    }

    private RouteState routeState() {
        return jdbcTemplate.queryForObject("""
                SELECT player_route.current_step_id,
                       player_route.status,
                       player_route.version,
                       player_route.completed_at
                FROM player_quest_routes player_route
                JOIN quest_routes route ON route.id = player_route.route_id
                WHERE player_route.player_id = ?
                  AND route.code = 'ROUTE_RECORD_START'
                """, (resultSet, rowNumber) -> new RouteState(
                resultSet.getLong("current_step_id"),
                resultSet.getString("status"),
                resultSet.getLong("version"),
                resultSet.getObject("completed_at")
        ), PLAYER_ID);
    }

    private record RouteState(
            long currentStepId,
            String status,
            long version,
            Object completedAt
    ) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        @Primary
        MutableClock questNotificationClock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> current;
        private final ZoneId zone;

        MutableClock() {
            this(new AtomicReference<>(ACCEPTED_AT), ZoneOffset.UTC);
        }

        private MutableClock(
                AtomicReference<Instant> current,
                ZoneId zone
        ) {
            this.current = current;
            this.zone = Objects.requireNonNull(zone, "zone");
        }

        void set(Instant instant) {
            current.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current, zone);
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
