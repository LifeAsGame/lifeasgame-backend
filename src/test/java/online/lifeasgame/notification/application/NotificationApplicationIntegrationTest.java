package online.lifeasgame.notification.application;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.notification.application.internal.NotificationAppendApi;
import online.lifeasgame.notification.application.result.NotificationResult;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.notification.domain.error.NotificationError;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=6")
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Current Player Notification application/persistence")
class NotificationApplicationIntegrationTest {

    private static final Long PLAYER_ID = 29201L;
    private static final Long OTHER_PLAYER_ID = 29202L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-21T10:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_notification")
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
    }

    @Autowired
    private NotificationAppendApi appendApi;

    @Autowired
    private NotificationQueryService queryService;

    @Autowired
    private NotificationReadMarker readMarker;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    private final AtomicLong currentPlayerId = new AtomicLong(PLAYER_ID);

    @BeforeEach
    void cleanState() {
        jdbc.update("DELETE FROM player_notifications");
        asCurrent(PLAYER_ID);
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willAnswer(invocation -> currentPlayerId.get());
    }

    @Nested
    @DisplayName("Notification-owned append API로 알림을 추가할 때")
    class AppendNotification {

        @Test
        @DisplayName("durable row를 만들고 같은 player replay만 무시한다")
        void appendsDurablyAndScopesReplayByPlayer() {
            assertThat(flyway.info().current().getVersion().getVersion())
                    .isEqualTo("28");

            append(PLAYER_ID, "shared-event");
            append(PLAYER_ID, "shared-event");
            append(OTHER_PLAYER_ID, "shared-event");

            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM player_notifications",
                    Integer.class
            )).isEqualTo(2);
            Map<String, Object> row = jdbc.queryForMap("""
                    SELECT player_id, type, title, body, source_event_id,
                           occurred_at, read_at, created_at, updated_at
                    FROM player_notifications
                    WHERE player_id = ?
                    """, PLAYER_ID);
            assertThat(row)
                    .containsEntry("player_id", PLAYER_ID)
                    .containsEntry("type", "SYSTEM_NOTICE")
                    .containsEntry("title", "알림 shared-event")
                    .containsEntry("body", "본문 shared-event")
                    .containsEntry("source_event_id", "shared-event")
                    .containsEntry("read_at", null)
                    .containsKeys("occurred_at", "created_at", "updated_at");
            assertThat(row.get("occurred_at")).isNotNull();
            assertThat(queryService.inbox(null, 20).notifications().getFirst()
                    .occurredAt()).isEqualTo(OCCURRED_AT);
        }

        @Test
        @DisplayName("동시 replay도 DB unique constraint로 한 row만 남긴다")
        void appendsConcurrentReplayOnce() throws Exception {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                List<Future<?>> attempts = List.of(
                        executor.submit(() -> concurrentAppend(ready, start)),
                        executor.submit(() -> concurrentAppend(ready, start))
                );
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                for (Future<?> attempt : attempts) {
                    attempt.get(10, TimeUnit.SECONDS);
                }
            } finally {
                executor.shutdownNow();
            }

            assertThat(notificationCount(PLAYER_ID, "concurrent-event"))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Current Player inbox를 조회할 때")
    class QueryInbox {

        @Test
        @DisplayName("own 알림을 ID 역순 cursor page와 올바른 nextCursor로 반환한다")
        void returnsOwnedNewestFirstCursorPage() {
            append(PLAYER_ID, "event-1");
            append(PLAYER_ID, "event-2");
            append(PLAYER_ID, "event-3");
            append(OTHER_PLAYER_ID, "other-event");
            List<Long> ownIds = notificationIds(PLAYER_ID);

            NotificationResult.Page first = queryService.inbox(null, 2);
            NotificationResult.Page second = queryService.inbox(
                    first.nextCursor(),
                    2
            );

            assertThat(first.notifications())
                    .extracting(NotificationResult.Info::id)
                    .containsExactly(ownIds.get(0), ownIds.get(1));
            assertThat(first.hasMore()).isTrue();
            assertThat(first.nextCursor()).isEqualTo(ownIds.get(1));
            assertThat(second.notifications())
                    .extracting(NotificationResult.Info::id)
                    .containsExactly(ownIds.get(2));
            assertThat(second.hasMore()).isFalse();
            assertThat(second.nextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("Current Player 알림을 읽음 처리할 때")
    class MarkRead {

        @Test
        @DisplayName("own unread 하나만 읽고 replay와 cross-owner를 안전하게 처리한다")
        void marksOneIdempotentlyAndHidesOwnership() {
            Long firstId = appendAndGetId(PLAYER_ID, "first");
            append(PLAYER_ID, "second");
            Long otherId = appendAndGetId(OTHER_PLAYER_ID, "other");

            readMarker.markOne(firstId);
            Instant firstReadAt = readAt(firstId);
            readMarker.markOne(firstId);

            assertThat(readAt(firstId)).isEqualTo(firstReadAt);
            assertThat(queryService.unreadCount()).isEqualTo(1);
            assertNotFound(otherId);
            assertNotFound(999_999L);
            assertThat(readAt(otherId)).isNull();
        }

        @Test
        @DisplayName("mark-all은 own unread만 변경하고 이미 읽은 시각을 유지한다")
        void marksAllOwnedUnreadOnly() {
            Long alreadyReadId = appendAndGetId(PLAYER_ID, "already-read");
            append(PLAYER_ID, "unread");
            Long otherId = appendAndGetId(OTHER_PLAYER_ID, "other-unread");
            readMarker.markOne(alreadyReadId);
            Instant originalReadAt = readAt(alreadyReadId);

            assertThat(readMarker.markAll()).isEqualTo(1);
            assertThat(readMarker.markAll()).isZero();

            assertThat(readAt(alreadyReadId)).isEqualTo(originalReadAt);
            assertThat(queryService.unreadCount()).isZero();
            assertThat(readAt(otherId)).isNull();
        }
    }

    private void concurrentAppend(CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent append start timed out");
            }
            append(PLAYER_ID, "concurrent-event");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Long appendAndGetId(Long playerId, String sourceEventId) {
        append(playerId, sourceEventId);
        return notificationIds(playerId).getFirst();
    }

    private void append(Long playerId, String sourceEventId) {
        appendApi.append(new NotificationAppendApi.AppendCommand(
                playerId,
                sourceEventId,
                NotificationType.SYSTEM_NOTICE,
                "알림 " + sourceEventId,
                "본문 " + sourceEventId,
                OCCURRED_AT
        ));
    }

    private void asCurrent(Long playerId) {
        currentPlayerId.set(playerId);
    }

    private List<Long> notificationIds(Long playerId) {
        return jdbc.queryForList(
                "SELECT id FROM player_notifications WHERE player_id = ? ORDER BY id DESC",
                Long.class,
                playerId
        );
    }

    private int notificationCount(Long playerId, String sourceEventId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM player_notifications
                WHERE player_id = ? AND source_event_id = ?
                """, Integer.class, playerId, sourceEventId);
    }

    private Instant readAt(Long notificationId) {
        Timestamp value = jdbc.queryForObject(
                "SELECT read_at FROM player_notifications WHERE id = ?",
                Timestamp.class,
                notificationId
        );
        return value == null ? null : value.toInstant();
    }

    private void assertNotFound(Long notificationId) {
        assertThatThrownBy(() -> readMarker.markOne(notificationId))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(NotificationError.NOTIFICATION_NOT_FOUND)
                );
    }
}
