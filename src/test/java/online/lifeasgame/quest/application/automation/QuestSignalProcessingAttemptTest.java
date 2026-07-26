package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import online.lifeasgame.quest.domain.repository.QuestSignalReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
@DisplayName("QuestSignalProcessingAttempt")
class QuestSignalProcessingAttemptTest {

    private static final Long QUEST_ID = 195L;
    private static final Long PLAYER_ID = 1950L;
    private static final Long RECEIPT_ID = 19500L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T03:00:00Z");
    private static final String FINGERPRINT = "a".repeat(64);

    @Mock
    private QuestSignalReceiptRepository receiptRepository;

    @Mock
    private QuestService questService;

    @Mock
    private QuestAcceptanceRepository questAcceptanceRepository;

    @Mock
    private QuestProgressStore questProgressStore;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private QuestSignalProcessingAttempt attempt;

    @BeforeEach
    void setUp() {
        attempt = new QuestSignalProcessingAttempt(
                receiptRepository,
                questService,
                questAcceptanceRepository,
                questProgressStore,
                domainEventPublisher
        );
    }

    @Nested
    @DisplayName("AUTO Quest Signal을 최초 적용할 때")
    class ApplyAutoSignal {

        @Test
        @DisplayName("Receipt를 먼저 flush하고 Goal과 Completed Event를 순서대로 발행한다")
        void savesReceiptBeforeApplyingQuest() {
            Quest quest = profileQuest();
            QuestSignal signal = signal(OCCURRED_AT);
            stubReceipt(signal);
            stubNewAcceptance(quest, signal);

            QuestSignalProcessingResult result =
                    attempt.process(signal, FINGERPRINT);

            assertThat(result).isEqualTo(
                    QuestSignalProcessingResult.applied(RECEIPT_ID)
            );
            InOrder order = inOrder(receiptRepository, questService);
            order.verify(receiptRepository).saveAndFlush(any());
            order.verify(questService).ensureQuest(signal.questCode());

            List<QuestEvent> events = publishedEvents(4);
            assertThat(events).extracting(QuestEvent::type)
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS,
                            QuestEventType.QUEST_GOAL_REACHED,
                            QuestEventType.QUEST_COMPLETED
                    );
            QuestEvent completed = events.getLast();
            assertThat(completed.attributes())
                    .containsEntry("questDefinitionVersion", 5)
                    .containsEntry("rewardProfileCode", "RP_EXP_10")
                    .doesNotContainKeys(
                            "rewardExp",
                            "rewardStats",
                            "rewardLines",
                            "rewardProfileId"
                    );
            assertThat(completed.correlationId())
                    .isEqualTo("source:signal-195:completed");
            QuestAcceptance saved = lastSavedAcceptance();
            assertThat(saved.getStatus()).isEqualTo(QuestStatus.COMPLETED);
            assertThat(saved.getGoalReachedAt()).isEqualTo(OCCURRED_AT);
            assertThat(saved.getCompletedAt()).isEqualTo(OCCURRED_AT);
            verify(questProgressStore).reset(
                    QuestCode.PLAYER_WELCOME,
                    PLAYER_ID
            );
        }
    }

    @Nested
    @DisplayName("USER_CONFIRM Quest Signal을 최초 적용할 때")
    class ApplyUserConfirmSignal {

        @Test
        @DisplayName("Goal Event까지만 발행하고 GOAL_REACHED를 유지한다")
        void waitsForExplicitCompletion() {
            Quest quest = quest(
                    QuestCompletionPolicy.USER_CONFIRM,
                    QuestRepeatRule.NONE
            );
            QuestSignal signal = signal(OCCURRED_AT);
            stubReceipt(signal);
            stubNewAcceptance(quest, signal);

            attempt.process(signal, FINGERPRINT);

            assertThat(publishedEvents(3)).extracting(QuestEvent::type)
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS,
                            QuestEventType.QUEST_GOAL_REACHED
                    );
            assertThat(lastSavedAcceptance().getStatus())
                    .isEqualTo(QuestStatus.GOAL_REACHED);
            verify(questProgressStore).reset(
                    QuestCode.PLAYER_WELCOME,
                    PLAYER_ID
            );
        }
    }

    @Nested
    @DisplayName("목표 미달 Signal을 적용할 때")
    class ApplyProgressSignal {

        @Test
        @DisplayName("DB Progress를 기준으로 누적하고 ProgressStore를 동기화한다")
        void accumulatesFromAcceptance() {
            Quest quest = quest(
                    QuestCompletionPolicy.AUTO,
                    QuestRepeatRule.NONE,
                    10
            );
            QuestSignal signal = signal(OCCURRED_AT);
            QuestAcceptance acceptance = acceptance(
                    quest,
                    TimePeriod.forever(),
                    900L
            );
            acceptance.setProgress(2, quest, OCCURRED_AT.minusSeconds(60));
            stubReceipt(signal);
            given(questService.ensureQuest(signal.questCode())).willReturn(quest);
            given(questAcceptanceRepository.findLatestByQuestAndPlayer(
                    QUEST_ID,
                    PLAYER_ID
            )).willReturn(Optional.of(acceptance));
            given(questAcceptanceRepository.save(any())).willAnswer(
                    invocation -> invocation.getArgument(0)
            );

            attempt.process(signal, FINGERPRINT);

            assertThat(acceptance.getProgressValue()).isEqualTo(3);
            assertThat(acceptance.getStatus())
                    .isEqualTo(QuestStatus.IN_PROGRESS);
            verify(questProgressStore).set(
                    QuestCode.PLAYER_WELCOME,
                    PLAYER_ID,
                    3,
                    null
            );
        }
    }

    @Nested
    @DisplayName("이미 전이가 끝난 Acceptance에 새 Signal이 들어올 때")
    class IgnoreTerminalAcceptance {

        @Test
        @DisplayName("Receipt만 저장하고 Quest mutation과 Event는 수행하지 않는다")
        void storesReceiptWithoutMutation() {
            Quest quest = quest(
                    QuestCompletionPolicy.USER_CONFIRM,
                    QuestRepeatRule.NONE
            );
            QuestAcceptance acceptance = acceptance(
                    quest,
                    TimePeriod.forever(),
                    901L
            );
            acceptance.reachGoal(OCCURRED_AT.minusSeconds(60));
            QuestSignal signal = signal(OCCURRED_AT);
            stubReceipt(signal);
            given(questService.ensureQuest(signal.questCode())).willReturn(quest);
            given(questAcceptanceRepository.findLatestByQuestAndPlayer(
                    QUEST_ID,
                    PLAYER_ID
            )).willReturn(Optional.of(acceptance));

            QuestSignalProcessingResult result =
                    attempt.process(signal, FINGERPRINT);

            assertThat(result.outcome())
                    .isEqualTo(QuestSignalProcessingResult.Outcome.APPLIED);
            verify(questAcceptanceRepository, never()).save(any());
            verifyNoInteractions(questProgressStore, domainEventPublisher);
        }
    }

    @Nested
    @DisplayName("반복 Quest의 이전 기간이 완료됐을 때")
    class StartNextPeriod {

        @Test
        @DisplayName("새 기간 Acceptance를 만들고 AUTO 완료 흐름을 유지한다")
        void createsNextAcceptance() {
            Instant now = Instant.now();
            LocalDate eventDate = now.atZone(ZoneId.systemDefault())
                    .toLocalDate();
            Quest quest = quest(
                    QuestCompletionPolicy.AUTO,
                    QuestRepeatRule.DAILY
            );
            QuestAcceptance previous = acceptance(
                    quest,
                    TimePeriod.daily(eventDate.minusDays(1)),
                    902L
            );
            previous.reachGoal(now.minusSeconds(120));
            previous.complete(now.minusSeconds(60));
            QuestSignal signal = signal(now);
            AtomicLong generatedId = new AtomicLong(1000L);
            stubReceipt(signal);
            given(questService.ensureQuest(signal.questCode())).willReturn(quest);
            given(questAcceptanceRepository.findLatestByQuestAndPlayer(
                    QUEST_ID,
                    PLAYER_ID
            )).willReturn(Optional.of(previous));
            given(questAcceptanceRepository.save(any())).willAnswer(
                    invocation -> {
                        QuestAcceptance value = invocation.getArgument(0);
                        if (value.getId() == null) {
                            ReflectionTestUtils.setField(
                                    value,
                                    "id",
                                    generatedId.getAndIncrement()
                            );
                        }
                        return value;
                    }
            );

            attempt.process(signal, FINGERPRINT);

            QuestAcceptance current = lastSavedAcceptance();
            assertThat(current.getId()).isNotEqualTo(previous.getId());
            assertThat(current.getPeriod().start()).isEqualTo(eventDate);
            assertThat(current.getStatus())
                    .isEqualTo(QuestStatus.COMPLETED);
            List<QuestEvent> events = publishedEvents(4);
            assertThat(events).extracting(QuestEvent::type)
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS,
                            QuestEventType.QUEST_GOAL_REACHED,
                            QuestEventType.QUEST_COMPLETED
                    );
            QuestEvent completed = events.getLast();
            assertThat(completed.attributes())
                    .containsEntry("questDefinitionVersion", 1)
                    .containsEntry("rewardExp", 15)
                    .containsEntry(
                            "rewardStats",
                            java.util.Map.of("focus", 1)
                    )
                    .doesNotContainKeys(
                            "rewardProfileCode",
                            "rewardLines",
                            "rewardProfileId"
                    );
            assertThat(completed.correlationId())
                    .isEqualTo("source:signal-195:completed");
            verify(questProgressStore).reset(
                    QuestCode.PLAYER_WELCOME,
                    PLAYER_ID
            );
        }
    }

    private void stubReceipt(QuestSignal signal) {
        given(receiptRepository.saveAndFlush(any())).willAnswer(invocation -> {
            QuestSignalReceipt receipt = invocation.getArgument(0);
            ReflectionTestUtils.setField(receipt, "id", RECEIPT_ID);
            assertThat(receipt.getQuestCode())
                    .isEqualTo(signal.questCode().value());
            assertThat(receipt.getCorrelationId())
                    .isEqualTo(signal.correlationId());
            assertThat(receipt.getPayloadFingerprint())
                    .isEqualTo(FINGERPRINT);
            return receipt;
        });
    }

    private void stubNewAcceptance(Quest quest, QuestSignal signal) {
        given(questService.ensureQuest(signal.questCode())).willReturn(quest);
        given(questAcceptanceRepository.findLatestByQuestAndPlayer(
                QUEST_ID,
                PLAYER_ID
        )).willReturn(Optional.empty());
        given(questAcceptanceRepository.save(any())).willAnswer(invocation -> {
            QuestAcceptance value = invocation.getArgument(0);
            if (value.getId() == null) {
                ReflectionTestUtils.setField(value, "id", 1000L);
            }
            return value;
        });
    }

    private QuestAcceptance lastSavedAcceptance() {
        ArgumentCaptor<QuestAcceptance> captor =
                ArgumentCaptor.forClass(QuestAcceptance.class);
        verify(questAcceptanceRepository, atLeastOnce())
                .save(captor.capture());
        return captor.getValue();
    }

    private List<QuestEvent> publishedEvents(int count) {
        ArgumentCaptor<DomainEvent> captor =
                ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher, times(count))
                .publish(captor.capture());
        return captor.getAllValues().stream()
                .map(QuestEvent.class::cast)
                .toList();
    }

    private QuestSignal signal(Instant occurredAt) {
        return QuestSignal.addProgress(
                        QuestCode.PLAYER_WELCOME,
                        PLAYER_ID,
                        1
                )
                .occurredAt(occurredAt)
                .correlationId("source:signal-195")
                .attribute("signalSource", "test")
                .build();
    }

    private QuestAcceptance acceptance(
            Quest quest,
            TimePeriod period,
            Long id
    ) {
        QuestAcceptance acceptance = QuestAcceptance.start(
                quest.getId(),
                PLAYER_ID,
                period
        );
        ReflectionTestUtils.setField(acceptance, "id", id);
        return acceptance;
    }

    private Quest quest(
            QuestCompletionPolicy completionPolicy,
            QuestRepeatRule repeatRule
    ) {
        return quest(completionPolicy, repeatRule, 1);
    }

    private Quest quest(
            QuestCompletionPolicy completionPolicy,
            QuestRepeatRule repeatRule,
            int target
    ) {
        Quest quest = Quest.create(
                QuestCode.PLAYER_WELCOME.value(),
                QuestCategory.MAIN,
                QuestTitle.of("Receipt 상태 계약"),
                "Quest Signal Receipt 상태 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, target),
                QuestReward.of(
                        15,
                        new RewardStats(java.util.Map.of("focus", 1))
                ),
                repeatRule,
                completionPolicy,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }

    private Quest profileQuest() {
        Quest quest = Quest.createDefinition(
                QuestCode.PLAYER_WELCOME.value(),
                5,
                QuestCategory.MAIN,
                QuestTitle.of("Profile Signal 계약"),
                "RewardProfile Quest Signal 계약 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                RewardProfileRef.of("RP_EXP_10"),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.AUTO,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }
}
