package online.lifeasgame.quest.application.event;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
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

@DisplayName("QuestRewardReadyPublisher")
class QuestRewardReadyPublisherTest {

    private static final Instant COMPLETED_AT =
            Instant.parse("2026-07-30T03:00:00Z");
    private static final Instant READY_AT =
            Instant.parse("2026-07-30T03:00:01Z");

    @Test
    @DisplayName("QUEST_COMPLETED를 typed reward-ready fact로 변환한다")
    void publishesTypedFactWithPreservedIdentity() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        QuestRewardReadyPublisher readyPublisher =
                new QuestRewardReadyPublisher(
                        publisher,
                        Clock.fixed(READY_AT, ZoneOffset.UTC)
                );
        QuestEvent completed = event(QuestEventType.QUEST_COMPLETED);

        readyPublisher.onQuestEvent(completed);

        ArgumentCaptor<QuestRewardReadyFact> captor =
                ArgumentCaptor.forClass(QuestRewardReadyFact.class);
        verify(publisher).publish(captor.capture());
        QuestRewardReadyFact fact = captor.getValue();
        assertThat(fact.eventVersion())
                .isEqualTo(QuestRewardReadyFact.EVENT_VERSION);
        assertThat(fact.playerId()).isEqualTo(2190L);
        assertThat(fact.acceptanceId()).isEqualTo(21900L);
        assertThat(fact.rewardProfileCode()).isEqualTo("RP_EXP_TINY_10");
        assertThat(fact.questId()).isEqualTo(219L);
        assertThat(fact.questCode()).isEqualTo("Q_FIRST_STEP");
        assertThat(fact.questDefinitionVersion()).isEqualTo(7);
        assertThat(fact.occurredAt()).isEqualTo(READY_AT);
        assertThat(fact.correlationId())
                .isEqualTo("manual-check:219:completed:reward");
    }

    @Test
    @DisplayName("legacy QUEST_REWARD_READY decode는 typed fact로 승격한다")
    void upgradesLegacyRewardReadyEvent() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        QuestRewardReadyPublisher readyPublisher =
                new QuestRewardReadyPublisher(
                        publisher,
                        Clock.fixed(READY_AT, ZoneOffset.UTC)
                );

        readyPublisher.onQuestEvent(event(QuestEventType.QUEST_REWARD_READY));

        ArgumentCaptor<QuestRewardReadyFact> captor =
                ArgumentCaptor.forClass(QuestRewardReadyFact.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().occurredAt()).isEqualTo(COMPLETED_AT);
        assertThat(captor.getValue().correlationId())
                .isEqualTo("manual-check:219:completed");
    }

    @Test
    @DisplayName("reward transition이 아닌 Quest Event는 무시한다")
    void ignoresOtherQuestEvents() {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        QuestRewardReadyPublisher readyPublisher =
                new QuestRewardReadyPublisher(
                        publisher,
                        Clock.fixed(READY_AT, ZoneOffset.UTC)
                );

        readyPublisher.onQuestEvent(event(QuestEventType.QUEST_PROGRESS));

        verifyNoInteractions(publisher);
    }

    private QuestEvent event(QuestEventType type) {
        return new QuestEvent(
                type,
                2190L,
                219L,
                "Q_FIRST_STEP",
                Map.of(
                        "acceptanceId", 21900L,
                        "rewardProfileCode", "RP_EXP_TINY_10",
                        "questDefinitionVersion", 7,
                        "source", "MANUAL_CHECK"
                ),
                COMPLETED_AT,
                "manual-check:219:completed"
        );
    }
}
