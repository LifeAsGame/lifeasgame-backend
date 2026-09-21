package online.lifeasgame.notification.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.notification.domain.error.NotificationError;

/** Approved ko-KR copy: AUTH-CFC-001-v1 / AUTH-CONT-001-v1.0.0. */
public record NotificationCopy(
        String title, String body, String titleCopyId, int titleCopyVersion,
        String bodyCopyId, int bodyCopyVersion, String locale
) {
    public static NotificationCopy forQuest(NotificationType type, String questTitle) {
        if (type == null) throw new DomainException(NotificationError.TYPE_REQUIRED);
        if (type != NotificationType.QUEST_COMPLETED && type != NotificationType.QUEST_REWARD_READY) {
            throw new DomainException(NotificationError.SOURCE_NOT_ACTIVE);
        }
        if (questTitle == null || questTitle.isBlank()) {
            throw new DomainException(NotificationError.QUEST_TITLE_REQUIRED);
        }
        String key;
        String title;
        String body;
        if (type == NotificationType.QUEST_COMPLETED) {
            key = "notification.ntf_quest_completed";
            title = "Quest를 완료했어요";
            body = questTitle + " 완료 사실이 기록되었습니다.";
        } else {
            // COPY_AUTHORITY_ADDENDUM_2026-09-05
            key = "notification.ntf_quest_reward_ready";
            title = "Quest 보상이 준비됐어요";
            body = questTitle + "의 확인 가능한 보상이 준비되었습니다. Mailbox 또는 결과 화면에서 상태를 확인해 주세요.";
        }
        return new NotificationCopy(title, body, key + ".title", 1, key + ".body", 1, "ko-KR");
    }
}
