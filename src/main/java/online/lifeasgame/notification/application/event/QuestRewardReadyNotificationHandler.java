package online.lifeasgame.notification.application.event;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.notification.application.internal.NotificationAppendApi;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.platform.outbox.application.OutboxEventDelivery;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestRewardReadyNotificationHandler {

    private static final String TITLE = "퀘스트 보상 준비";
    private static final String BODY = "퀘스트 보상을 확인할 수 있습니다.";

    private final NotificationAppendApi notificationAppendApi;

    @EventListener
    public void onOutboxEvent(OutboxEventDelivery delivery) {
        if (!(delivery.event() instanceof QuestRewardReadyFact fact)) {
            return;
        }
        notificationAppendApi.append(new NotificationAppendApi.AppendCommand(
                fact.playerId(),
                delivery.eventId(),
                NotificationType.QUEST_REWARD_READY,
                TITLE,
                BODY,
                fact.occurredAt()
        ));
    }
}
