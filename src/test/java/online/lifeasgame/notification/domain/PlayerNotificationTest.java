package online.lifeasgame.notification.domain;

import java.time.Instant;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.notification.domain.error.NotificationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Player Notification approved copy domain")
class PlayerNotificationTest {
    private static final Instant AT = Instant.parse("2026-08-21T10:00:00Z");

    @ParameterizedTest
    @EnumSource(value = NotificationType.class, names = {"QUEST_COMPLETED", "QUEST_REWARD_READY"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("기존 이력 type이라도 활성 두 source 외 신규 생성은 거부한다")
    void rejectsInactiveSources(NotificationType type) {
        assertError(() -> PlayerNotification.create(1L, "event", type, "Quest", AT), NotificationError.SOURCE_NOT_ACTIVE);
    }

    @Test
    @DisplayName("필수 identity·제목 snapshot·시각을 검증하고 미확인 제목을 만들어 넣지 않는다")
    void validatesSource() {
        assertError(() -> create(null, "event", "Quest", AT), NotificationError.PLAYER_ID_REQUIRED);
        assertError(() -> create(1L, " ", "Quest", AT), NotificationError.SOURCE_EVENT_ID_REQUIRED);
        assertError(() -> create(1L, "x".repeat(256), "Quest", AT), NotificationError.SOURCE_EVENT_ID_TOO_LONG);
        assertError(() -> PlayerNotification.create(1L, "event", null, "Quest", AT), NotificationError.TYPE_REQUIRED);
        assertError(() -> create(1L, "event", null, AT), NotificationError.QUEST_TITLE_REQUIRED);
        assertError(() -> create(1L, "event", " ", AT), NotificationError.QUEST_TITLE_REQUIRED);
        assertError(() -> create(1L, "event", "Quest", null), NotificationError.OCCURRED_AT_REQUIRED);
    }

    @Test
    @DisplayName("승인 문구와 copy provenance는 읽음 재처리에도 유지된다")
    void preservesCopyOnRead() {
        PlayerNotification notification = create(1L, "event", "첫 기록", AT);
        assertThat(notification.getTitle()).isEqualTo("Quest를 완료했어요");
        assertThat(notification.getBody()).isEqualTo("첫 기록 완료 사실이 기록되었습니다.");
        assertThat(notification.getTitleCopyId()).isEqualTo("notification.ntf_quest_completed.title");
        assertThat(notification.getTitleCopyVersion()).isEqualTo(1);
        assertThat(notification.getBodyCopyId()).isEqualTo("notification.ntf_quest_completed.body");
        assertThat(notification.getBodyCopyVersion()).isEqualTo(1);
        assertThat(notification.getCopyLocale()).isEqualTo("ko-KR");
        assertThat(notification.getReadAt()).isNull();
        notification.markRead(AT.plusSeconds(1));
        notification.markRead(AT.plusSeconds(2));
        assertThat(notification.getReadAt()).isEqualTo(AT.plusSeconds(1));
        assertThat(notification.getBody()).isEqualTo("첫 기록 완료 사실이 기록되었습니다.");
    }

    private PlayerNotification create(Long player, String event, String title, Instant at) {
        return PlayerNotification.create(player, event, NotificationType.QUEST_COMPLETED, title, at);
    }

    private void assertError(Runnable action, NotificationError error) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(error));
    }
}
