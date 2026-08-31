package online.lifeasgame.notification.application.event;

import online.lifeasgame.notification.application.internal.NotificationAppendApi;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.platform.outbox.application.OutboxEventDelivery;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Quest completion notification handler")
class QuestCompletionNotificationHandlerTest {

    private static final String EVENT_ID =
            "31700000-0000-0000-0000-000000000001";
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-31T01:00:00Z");

    @Mock
    private NotificationAppendApi notificationAppendApi;

    @InjectMocks
    private QuestCompletionNotificationHandler handler;

    @Test
    @DisplayName("QuestCompleted replay를 같은 source identity와 완료 시각으로 append한다")
    void mapsCompletionReplay() {
        OutboxEventDelivery delivery = delivery(QuestEventType.QUEST_COMPLETED);

        handler.onOutboxEvent(delivery);
        handler.onOutboxEvent(delivery);

        ArgumentCaptor<NotificationAppendApi.AppendCommand> commands =
                ArgumentCaptor.forClass(
                        NotificationAppendApi.AppendCommand.class
                );
        verify(notificationAppendApi, times(2)).append(commands.capture());
        assertThat(commands.getAllValues()).containsExactly(
                expectedCommand(),
                expectedCommand()
        );
    }

    @Test
    @DisplayName("Quest reward-ready event에는 별도 알림을 만들지 않는다")
    void ignoresRewardReady() {
        handler.onOutboxEvent(delivery(QuestEventType.QUEST_REWARD_READY));

        verify(notificationAppendApi, never()).append(any());
    }

    @Test
    @DisplayName("Notification append 실패를 삼키지 않는다")
    void propagatesAppendFailure() {
        RuntimeException failure = new IllegalStateException("append failed");
        willThrow(failure).given(notificationAppendApi).append(any());

        assertThatThrownBy(() -> handler.onOutboxEvent(
                delivery(QuestEventType.QUEST_COMPLETED)
        )).isSameAs(failure);
    }

    private OutboxEventDelivery delivery(QuestEventType type) {
        return new OutboxEventDelivery(
                EVENT_ID,
                QuestEvent.builder(type)
                        .playerId(317L)
                        .questId(31L)
                        .attribute("acceptanceId", 3170L)
                        .occurredAt(COMPLETED_AT)
                        .correlationId(
                                "quest:31:acceptance:3170:completed"
                        )
                        .build()
        );
    }

    private NotificationAppendApi.AppendCommand expectedCommand() {
        return new NotificationAppendApi.AppendCommand(
                317L,
                EVENT_ID,
                NotificationType.QUEST_COMPLETED,
                "퀘스트 완료",
                "퀘스트를 완료했습니다.",
                COMPLETED_AT
        );
    }
}
