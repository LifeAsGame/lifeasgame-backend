package online.lifeasgame.notification.application.event;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.notification.application.internal.NotificationAppendApi;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.platform.outbox.application.OutboxEventDelivery;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestCompletionNotificationHandler {

    private final NotificationAppendApi notificationAppendApi;

    @EventListener
    public void onOutboxEvent(OutboxEventDelivery delivery) {
        if (!(delivery.event() instanceof QuestEvent event)
                || event.type() != QuestEventType.QUEST_COMPLETED) {
            return;
        }
        notificationAppendApi.append(new NotificationAppendApi.AppendCommand(
                event.playerId(),
                delivery.eventId(),
                NotificationType.QUEST_COMPLETED,
                event.attributes().get("questTitle") instanceof String title ? title : null,
                event.occurredAt()
        ));
    }
}
