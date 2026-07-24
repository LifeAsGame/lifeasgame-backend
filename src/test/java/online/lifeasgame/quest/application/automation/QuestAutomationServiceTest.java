package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestAutomationService")
class QuestAutomationServiceTest {

    private static final Long QUEST_ID = 193L;
    private static final Long PLAYER_ID = 1930L;
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-23T03:00:00Z");

    @Mock
    private QuestService questService;

    @Mock
    private QuestAcceptanceRepository questAcceptanceRepository;

    @Mock
    private QuestProgressStore questProgressStore;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private QuestSignalIdempotencyGuard questSignalIdempotencyGuard;

    private QuestAutomationService service;

    @BeforeEach
    void setUp() {
        service = new QuestAutomationService(
                questService,
                questAcceptanceRepository,
                questProgressStore,
                domainEventPublisher,
                questSignalIdempotencyGuard
        );
    }

    @Nested
    @DisplayName("AUTO Quest가 목표를 달성할 때")
    class CompleteAutomatically {

        @Test
        @DisplayName("Progress, Goal, Completed Event를 순서대로 발행하고 COMPLETED가 된다")
        void publishesOrderedEvents() {
            Quest quest = quest(QuestCompletionPolicy.AUTO, QuestRepeatRule.NONE);
            QuestSignal signal = signal(OCCURRED_AT);
            stubNewAcceptance(quest, signal);
            given(questProgressStore.set(
                    QuestCode.PLAYER_WELCOME,
                    PLAYER_ID,
                    1,
                    null
            )).willReturn(1);

            service.processSignals(List.of(signal));

            List<QuestEvent> events = publishedEvents(4);
            assertThat(events).extracting(QuestEvent::type)
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS,
                            QuestEventType.QUEST_GOAL_REACHED,
                            QuestEventType.QUEST_COMPLETED
                    );

            QuestEvent goalEvent = events.get(2);
            assertThat(goalEvent.questId()).isEqualTo(QUEST_ID);
            assertThat(goalEvent.questCode()).isEqualTo(quest.getCode());
            assertThat(goalEvent.playerId()).isEqualTo(PLAYER_ID);
            assertThat(goalEvent.correlationId()).isEqualTo("signal-193:goal-reached");
            assertThat(goalEvent.attributes())
                    .containsEntry("acceptanceId", 1000L)
                    .containsEntry("progress", 1)
                    .containsEntry("target", 1)
                    .containsEntry("reachedAt", OCCURRED_AT)
                    .containsEntry("completionPolicy", QuestCompletionPolicy.AUTO.name())
                    .containsEntry("signalSource", "test");

            QuestAcceptance saved = lastSavedAcceptance();
            assertThat(saved.getStatus()).isEqualTo(QuestStatus.COMPLETED);
            assertThat(saved.getGoalReachedAt()).isEqualTo(OCCURRED_AT);
            assertThat(saved.getCompletedAt()).isEqualTo(OCCURRED_AT);
        }
    }

    @Nested
    @DisplayName("USER_CONFIRM Quest가 목표를 달성할 때")
    class WaitForConfirmation {

        @Test
        @DisplayName("Goal Event까지만 발행하고 GOAL_REACHED를 유지한다")
        void doesNotPublishCompletedEvent() {
            Quest quest = quest(QuestCompletionPolicy.USER_CONFIRM, QuestRepeatRule.NONE);
            QuestSignal signal = signal(OCCURRED_AT);
            stubNewAcceptance(quest, signal);
            given(questProgressStore.set(
                    QuestCode.PLAYER_WELCOME,
                    PLAYER_ID,
                    1,
                    null
            )).willReturn(1);

            service.processSignals(List.of(signal));

            assertThat(publishedEvents(3)).extracting(QuestEvent::type)
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS,
                            QuestEventType.QUEST_GOAL_REACHED
                    );
            QuestAcceptance saved = lastSavedAcceptance();
            assertThat(saved.getStatus()).isEqualTo(QuestStatus.GOAL_REACHED);
            assertThat(saved.getCompletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("이미 목표에 도달했거나 완료한 Acceptance에 신호가 들어올 때")
    class IgnoreDuplicateTransition {

        @Test
        @DisplayName("GOAL_REACHED 상태에는 Progress와 Goal Event를 다시 발행하지 않는다")
        void ignoresGoalReachedAcceptance() {
            Quest quest = quest(QuestCompletionPolicy.USER_CONFIRM, QuestRepeatRule.NONE);
            QuestAcceptance acceptance = acceptance(quest, TimePeriod.forever(), 900L);
            acceptance.reachGoal(OCCURRED_AT.minusSeconds(60));
            QuestSignal signal = signal(OCCURRED_AT);
            given(questService.ensureQuest(signal.questCode())).willReturn(quest);
            given(questSignalIdempotencyGuard.accept(quest, signal)).willReturn(true);
            given(questAcceptanceRepository.findLatestByQuestAndPlayer(QUEST_ID, PLAYER_ID))
                    .willReturn(Optional.of(acceptance));

            service.processSignals(List.of(signal));

            verifyNoInteractions(questProgressStore, domainEventPublisher);
            verify(questAcceptanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("COMPLETED 상태에는 Completed Event를 다시 발행하지 않는다")
        void ignoresCompletedAcceptance() {
            Quest quest = quest(QuestCompletionPolicy.AUTO, QuestRepeatRule.NONE);
            QuestAcceptance acceptance = acceptance(quest, TimePeriod.forever(), 901L);
            acceptance.reachGoal(OCCURRED_AT.minusSeconds(120));
            acceptance.complete(OCCURRED_AT.minusSeconds(60));
            QuestSignal signal = signal(OCCURRED_AT);
            given(questService.ensureQuest(signal.questCode())).willReturn(quest);
            given(questSignalIdempotencyGuard.accept(quest, signal)).willReturn(true);
            given(questAcceptanceRepository.findLatestByQuestAndPlayer(QUEST_ID, PLAYER_ID))
                    .willReturn(Optional.of(acceptance));

            service.processSignals(List.of(signal));

            verifyNoInteractions(questProgressStore, domainEventPublisher);
            verify(questAcceptanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("반복 Quest의 이전 기간이 완료됐을 때")
    class StartNextRepeatPeriod {

        @Test
        @DisplayName("새 기간 Acceptance를 생성하고 기존 AUTO 완료 흐름을 유지한다")
        void createsNextAcceptance() {
            Instant now = Instant.now();
            LocalDate eventDate = now.atZone(ZoneId.systemDefault()).toLocalDate();
            Quest quest = quest(QuestCompletionPolicy.AUTO, QuestRepeatRule.DAILY);
            QuestAcceptance previous = acceptance(
                    quest,
                    TimePeriod.daily(eventDate.minusDays(1)),
                    902L
            );
            previous.reachGoal(now.minusSeconds(120));
            previous.complete(now.minusSeconds(60));
            QuestSignal signal = signal(now);
            AtomicLong generatedId = new AtomicLong(1000L);

            given(questService.ensureQuest(signal.questCode())).willReturn(quest);
            given(questSignalIdempotencyGuard.accept(quest, signal)).willReturn(true);
            given(questAcceptanceRepository.findLatestByQuestAndPlayer(QUEST_ID, PLAYER_ID))
                    .willReturn(Optional.of(previous));
            given(questAcceptanceRepository.save(any())).willAnswer(invocation -> {
                QuestAcceptance value = invocation.getArgument(0);
                if (value.getId() == null) {
                    ReflectionTestUtils.setField(value, "id", generatedId.getAndIncrement());
                }
                return value;
            });
            given(questProgressStore.set(
                    eq(QuestCode.PLAYER_WELCOME),
                    eq(PLAYER_ID),
                    eq(1),
                    any(Duration.class)
            )).willReturn(1);

            service.processSignals(List.of(signal));

            QuestAcceptance current = lastSavedAcceptance();
            assertThat(current.getId()).isNotEqualTo(previous.getId());
            assertThat(current.getPeriod().start()).isEqualTo(eventDate);
            assertThat(current.getStatus()).isEqualTo(QuestStatus.COMPLETED);
            assertThat(publishedEvents(4)).extracting(QuestEvent::type)
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS,
                            QuestEventType.QUEST_GOAL_REACHED,
                            QuestEventType.QUEST_COMPLETED
                    );
        }
    }

    private void stubNewAcceptance(Quest quest, QuestSignal signal) {
        given(questService.ensureQuest(signal.questCode())).willReturn(quest);
        given(questSignalIdempotencyGuard.accept(quest, signal)).willReturn(true);
        given(questAcceptanceRepository.findLatestByQuestAndPlayer(QUEST_ID, PLAYER_ID))
                .willReturn(Optional.empty());
        given(questAcceptanceRepository.save(any())).willAnswer(invocation -> {
            QuestAcceptance value = invocation.getArgument(0);
            if (value.getId() == null) {
                ReflectionTestUtils.setField(value, "id", 1000L);
            }
            return value;
        });
    }

    private QuestAcceptance lastSavedAcceptance() {
        ArgumentCaptor<QuestAcceptance> captor = ArgumentCaptor.forClass(QuestAcceptance.class);
        verify(questAcceptanceRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private List<QuestEvent> publishedEvents(int count) {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher, times(count)).publish(captor.capture());
        return captor.getAllValues().stream()
                .map(QuestEvent.class::cast)
                .toList();
    }

    private QuestSignal signal(Instant occurredAt) {
        return QuestSignal.setProgress(QuestCode.PLAYER_WELCOME, PLAYER_ID, 1)
                .occurredAt(occurredAt)
                .correlationId("signal-193")
                .attribute("signalSource", "test")
                .build();
    }

    private QuestAcceptance acceptance(Quest quest, TimePeriod period, Long id) {
        QuestAcceptance acceptance = QuestAcceptance.start(quest.getId(), PLAYER_ID, period);
        ReflectionTestUtils.setField(acceptance, "id", id);
        return acceptance;
    }

    private Quest quest(
            QuestCompletionPolicy completionPolicy,
            QuestRepeatRule repeatRule
    ) {
        Quest quest = Quest.create(
                QuestCode.PLAYER_WELCOME.value(),
                QuestCategory.MAIN,
                QuestTitle.of("자동화 상태 계약"),
                "Quest Automation 상태 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(0, RewardStats.empty()),
                repeatRule,
                completionPolicy,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }
}
