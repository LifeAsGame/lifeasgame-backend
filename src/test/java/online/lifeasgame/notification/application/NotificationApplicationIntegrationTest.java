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
                    .isEqualTo("35");

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
                    .containsEntry("type", "QUEST_COMPLETED")
                    .containsEntry("title", "Quest를 완료했어요")
                    .containsEntry("body", "테스트 Quest 완료 사실이 기록되었습니다.")
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

    @Test
    @DisplayName("legacy replay는 제목 snapshot이 없어도 원문과 null provenance를 보존하고 읽음 처리가 가능하다")
    void preservesLegacyReplay() {
        jdbc.update("""
                INSERT INTO player_notifications (player_id, type, title, body, source_event_id,
                    occurred_at, created_at, updated_at)
                VALUES (?, 'QUEST_COMPLETED', '기존 제목', '기존 본문', 'legacy-event', NOW(6), NOW(6), NOW(6))
                """, PLAYER_ID);
        var before = jdbc.queryForMap("SELECT * FROM player_notifications");
        appendApi.append(new NotificationAppendApi.AppendCommand(PLAYER_ID, "legacy-event",
                NotificationType.QUEST_COMPLETED, null, OCCURRED_AT));
        assertThat(jdbc.queryForMap("SELECT * FROM player_notifications")).isEqualTo(before);
        var notification = queryService.inbox(null, 20).notifications().getFirst();
        assertThat(notification.title()).isEqualTo("기존 제목");
        assertThat(notification.body()).isEqualTo("기존 본문");
        assertThat(notification.titleCopyId()).isNull();
        assertThat(notification.bodyCopyVersion()).isNull();
        readMarker.markOne(notification.id());
        assertThat(queryService.unreadCount()).isZero();
    }

    @Test
    @DisplayName("새 알림은 승인 copy metadata가 일치하며 replay의 제목 변경은 저장 내용을 변경하지 않는다")
    void preservesRenderedCopyOnReplay() {
        append(PLAYER_ID, "immutable-copy");
        var before = jdbc.queryForMap("SELECT * FROM player_notifications");
        appendApi.append(new NotificationAppendApi.AppendCommand(PLAYER_ID, "immutable-copy",
                NotificationType.QUEST_COMPLETED, "나중에 변경한 Quest", OCCURRED_AT));
        assertThat(jdbc.queryForMap("SELECT * FROM player_notifications")).isEqualTo(before);
        var notification = queryService.inbox(null, 20).notifications().getFirst();
        assertThat(notification.titleCopyId()).isEqualTo("notification.ntf_quest_completed.title");
        assertThat(notification.titleCopyVersion()).isEqualTo(1);
        assertThat(notification.bodyCopyId()).isEqualTo("notification.ntf_quest_completed.body");
        assertThat(notification.bodyCopyVersion()).isEqualTo(1);
        assertThat(notification.copyLocale()).isEqualTo("ko-KR");
        assertThat(notification.body()).isEqualTo("테스트 Quest 완료 사실이 기록되었습니다.");
    }

    @Test
    @DisplayName("비활성 source와 제목 없는 신규 source는 영속 부작용 없이 거부한다")
    void rejectsUnapprovedEmission() {
        assertThatThrownBy(() -> appendApi.append(new NotificationAppendApi.AppendCommand(PLAYER_ID,
                "inactive", NotificationType.SYSTEM_NOTICE, "Quest", OCCURRED_AT)))
                .isInstanceOfSatisfying(DomainException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(NotificationError.SOURCE_NOT_ACTIVE));
        assertThatThrownBy(() -> appendApi.append(new NotificationAppendApi.AppendCommand(PLAYER_ID,
                "missing-title", NotificationType.QUEST_COMPLETED, null, OCCURRED_AT)))
                .isInstanceOfSatisfying(DomainException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(NotificationError.QUEST_TITLE_REQUIRED));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM player_notifications", Long.class)).isZero();
    }

    @Test
    @DisplayName("이전에 저장된 미지원 type도 이력과 읽음을 유지하되 신규 생성 경계와 분리한다")
    void readsUnknownStoredType() {
        // Simulate imported/future history in this disposable DB; production constraint is unchanged.
        jdbc.execute("ALTER TABLE player_notifications ALTER CHECK ck_player_notification_type NOT ENFORCED");
        try {
            jdbc.update("""
                    INSERT INTO player_notifications (player_id, type, title, body, source_event_id,
                      occurred_at, created_at, updated_at)
                    VALUES (?, 'FUTURE_UNKNOWN', '이전 제목', '이전 본문', 'unknown-history', NOW(6), NOW(6), NOW(6))
                    """, PLAYER_ID);
            var info = queryService.inbox(null, 20).notifications().getFirst();
            assertThat(info.type()).isEqualTo("FUTURE_UNKNOWN");
            assertThat(info.copyLocale()).isNull();
            readMarker.markOne(info.id());
            assertThat(queryService.unreadCount()).isZero();
            asCurrent(OTHER_PLAYER_ID);
            assertThat(queryService.inbox(null, 20).notifications()).isEmpty();
            assertNotFound(info.id());
        } finally {
            jdbc.update("DELETE FROM player_notifications WHERE type = 'FUTURE_UNKNOWN'");
            jdbc.execute("ALTER TABLE player_notifications ALTER CHECK ck_player_notification_type ENFORCED");
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
                NotificationType.QUEST_COMPLETED,
                "테스트 Quest",
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
