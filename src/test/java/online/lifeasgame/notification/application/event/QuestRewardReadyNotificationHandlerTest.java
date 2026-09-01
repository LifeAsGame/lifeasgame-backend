package online.lifeasgame.notification.application.event;

import online.lifeasgame.notification.application.internal.NotificationAppendApi;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.platform.outbox.application.OutboxEventDelivery;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
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
@DisplayName("Quest reward-ready notification handler")
class QuestRewardReadyNotificationHandlerTest {

    private static final String EVENT_ID =
            "31900000-0000-0000-0000-000000000001";
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-09-01T01:00:00Z");

    @Mock
    private NotificationAppendApi notificationAppendApi;

    @InjectMocks
    private QuestRewardReadyNotificationHandler handler;

    @Test
    @DisplayName("typed reward-ready replay를 같은 canonical source identity로 append한다")
    void mapsRewardReadyReplay() {
        OutboxEventDelivery delivery = delivery();

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
    @DisplayName("generic QuestEvent attributes로 reward-ready 의미를 재구성하지 않는다")
    void ignoresGenericQuestEvent() {
        QuestEvent event = QuestEvent.builder(QuestEventType.QUEST_REWARD_READY)
                .playerId(319L)
                .questId(31L)
                .attribute("acceptanceId", 3190L)
                .attribute("rewardProfileCode", "RP_319")
                .attribute("questDefinitionVersion", 1)
                .occurredAt(OCCURRED_AT)
                .correlationId("quest:31:acceptance:3190:reward")
                .build();

        handler.onOutboxEvent(new OutboxEventDelivery(EVENT_ID, event));

        verify(notificationAppendApi, never()).append(any());
    }

    @Test
    @DisplayName("Notification append 실패를 outbox relay 경계로 전파한다")
    void propagatesAppendFailure() {
        RuntimeException failure = new IllegalStateException("append failed");
        willThrow(failure).given(notificationAppendApi).append(any());

        assertThatThrownBy(() -> handler.onOutboxEvent(delivery()))
                .isSameAs(failure);
    }

    private OutboxEventDelivery delivery() {
        return new OutboxEventDelivery(
                EVENT_ID,
                new QuestRewardReadyFact(
                        QuestRewardReadyFact.EVENT_VERSION,
                        319L,
                        3190L,
                        "RP_319",
                        31L,
                        "Q_319",
                        1,
                        OCCURRED_AT,
                        "quest:31:acceptance:3190:reward"
                )
        );
    }

    private NotificationAppendApi.AppendCommand expectedCommand() {
        return new NotificationAppendApi.AppendCommand(
                319L,
                EVENT_ID,
                NotificationType.QUEST_REWARD_READY,
                "퀘스트 보상 준비",
                "퀘스트 보상을 확인할 수 있습니다.",
                OCCURRED_AT
        );
    }
}
