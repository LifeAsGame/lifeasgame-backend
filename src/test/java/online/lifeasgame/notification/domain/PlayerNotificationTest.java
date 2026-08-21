package online.lifeasgame.notification.domain;

import java.time.Instant;
import java.util.stream.Stream;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.notification.domain.error.NotificationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Player Notification domain")
class PlayerNotificationTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-21T10:00:00Z");

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidNotifications")
    @DisplayName("필수 알림 값이 유효하지 않으면 생성하지 않는다")
    void rejectsInvalidRequiredValues(
            String scenario,
            Long playerId,
            String sourceEventId,
            NotificationType type,
            String title,
            String body,
            Instant occurredAt,
            NotificationError expected
    ) {
        assertThatThrownBy(() -> PlayerNotification.create(
                playerId,
                sourceEventId,
                type,
                title,
                body,
                occurredAt
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(expected)
        );
    }

    @Test
    @DisplayName("readAt null 여부만으로 unread와 read 상태를 표현한다")
    void marksReadIdempotently() {
        PlayerNotification notification = validNotification();
        Instant firstReadAt = Instant.parse("2026-08-21T11:00:00Z");

        assertThat(notification.getReadAt()).isNull();
        notification.markRead(firstReadAt);
        notification.markRead(Instant.parse("2026-08-21T12:00:00Z"));

        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    private static Stream<Arguments> invalidNotifications() {
        return Stream.of(
                Arguments.of(
                        "playerId null",
                        null, "event-1", NotificationType.SYSTEM_NOTICE,
                        "제목", "본문", OCCURRED_AT,
                        NotificationError.PLAYER_ID_REQUIRED
                ),
                Arguments.of(
                        "sourceEventId blank",
                        1L, " ", NotificationType.SYSTEM_NOTICE,
                        "제목", "본문", OCCURRED_AT,
                        NotificationError.SOURCE_EVENT_ID_REQUIRED
                ),
                Arguments.of(
                        "type null",
                        1L, "event-1", null,
                        "제목", "본문", OCCURRED_AT,
                        NotificationError.TYPE_REQUIRED
                ),
                Arguments.of(
                        "title blank",
                        1L, "event-1", NotificationType.SYSTEM_NOTICE,
                        " ", "본문", OCCURRED_AT,
                        NotificationError.TITLE_REQUIRED
                ),
                Arguments.of(
                        "body blank",
                        1L, "event-1", NotificationType.SYSTEM_NOTICE,
                        "제목", " ", OCCURRED_AT,
                        NotificationError.BODY_REQUIRED
                ),
                Arguments.of(
                        "occurredAt null",
                        1L, "event-1", NotificationType.SYSTEM_NOTICE,
                        "제목", "본문", null,
                        NotificationError.OCCURRED_AT_REQUIRED
                )
        );
    }

    private static PlayerNotification validNotification() {
        return PlayerNotification.create(
                1L,
                "event-1",
                NotificationType.SYSTEM_NOTICE,
                "제목",
                "본문",
                OCCURRED_AT
        );
    }
}
