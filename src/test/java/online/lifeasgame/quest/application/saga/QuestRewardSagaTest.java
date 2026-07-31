package online.lifeasgame.quest.application.saga;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("QuestRewardSaga")
class QuestRewardSagaTest {

    private static final Instant COMPLETED_AT =
            Instant.parse("2026-07-30T03:00:00Z");
    private static final Instant READY_AT =
            Instant.parse("2026-07-30T03:00:01Z");

    @Test
    @DisplayName("QUEST_COMPLETED를 주입 Clock 시각의 QUEST_REWARD_READY로 변환한다")
    void schedulesRewardWithPreservedContextAndCorrelation() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        QuestRewardSaga saga = new QuestRewardSaga(
                publisher,
                Clock.fixed(READY_AT, ZoneOffset.UTC)
        );
        QuestEvent completed = new QuestEvent(
                QuestEventType.QUEST_COMPLETED,
                2190L,
                219L,
                "Q_FIRST_STEP",
                Map.of(
                        "acceptanceId", 21900L,
                        "rewardProfileCode", "RP_EXP_TINY_10",
                        "source", "MANUAL_CHECK"
                ),
                COMPLETED_AT,
                "manual-check:219:completed"
        );

        saga.onQuestEvent(completed);

        ArgumentCaptor<DomainEvent> captor =
                ArgumentCaptor.forClass(DomainEvent.class);
        verify(publisher).publish(captor.capture());
        QuestEvent ready = (QuestEvent) captor.getValue();
        assertThat(ready.type())
                .isEqualTo(QuestEventType.QUEST_REWARD_READY);
        assertThat(ready.questId()).isEqualTo(completed.questId());
        assertThat(ready.questCode()).isEqualTo(completed.questCode());
        assertThat(ready.playerId()).isEqualTo(completed.playerId());
        assertThat(ready.attributes()).isEqualTo(completed.attributes());
        assertThat(ready.occurredAt()).isEqualTo(READY_AT);
        assertThat(ready.correlationId())
                .isEqualTo("manual-check:219:completed:reward");
    }

    @Test
    @DisplayName("QUEST_COMPLETED 외 Event는 변환하지 않는다")
    void ignoresOtherQuestEvents() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        QuestRewardSaga saga = new QuestRewardSaga(
                publisher,
                Clock.fixed(READY_AT, ZoneOffset.UTC)
        );

        saga.onQuestEvent(new QuestEvent(
                QuestEventType.QUEST_PROGRESS,
                2190L,
                219L,
                "Q_FIRST_STEP",
                Map.of(),
                COMPLETED_AT,
                "quest:219:progress"
        ));

        verifyNoInteractions(publisher);
    }
}
