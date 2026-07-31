package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.*;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestAcceptanceCompletionService")
class QuestAcceptanceCompletionServiceTest {

    private static final Long QUEST_ID = 193L;
    private static final Long ACCEPTANCE_ID = 1930L;
    private static final Instant GOAL_REACHED_AT = Instant.parse("2026-07-23T03:00:00Z");
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-23T02:00:00Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-07-23T04:00:00Z");

    @Mock
    private QuestReader questReader;

    @Mock
    private QuestWriter questWriter;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private QuestAcceptanceCompletionService service;

    @BeforeEach
    void setUp() {
        service = new QuestAcceptanceCompletionService(
                questReader,
                questWriter,
                domainEventPublisher,
                Clock.fixed(COMPLETED_AT, ZoneOffset.UTC)
        );
    }

    @Nested
    @DisplayName("USER_CONFIRM Acceptance를 명시적으로 완료할 때")
    class CompleteUserConfirmedAcceptance {

        @Test
        @DisplayName("COMPLETED로 저장하고 Completed Event와 결과를 반환한다")
        void completesAndPublishesEvent() {
            Quest quest = profileQuest(QuestCompletionPolicy.USER_CONFIRM);
            QuestAcceptance acceptance = goalReachedAcceptance(quest);
            given(questReader.getAcceptanceForUpdate(ACCEPTANCE_ID))
                    .willReturn(acceptance);
            given(questReader.getById(QUEST_ID)).willReturn(quest);

            QuestResult.Acceptance result = service.complete(ACCEPTANCE_ID);

            assertThat(result.status()).isEqualTo(QuestStatus.COMPLETED.name());
            assertThat(result.progressValue()).isEqualTo(1);
            assertThat(result.completionPolicy())
                    .isEqualTo(QuestCompletionPolicy.USER_CONFIRM.name());
            assertThat(result.goalReachedAt()).isEqualTo(GOAL_REACHED_AT);
            assertThat(result.completedAt()).isEqualTo(COMPLETED_AT);
            verify(questWriter).saveAcceptance(acceptance);
            verify(questReader).getAcceptanceForUpdate(ACCEPTANCE_ID);

            ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
            verify(domainEventPublisher).publish(eventCaptor.capture());
            QuestEvent event = (QuestEvent) eventCaptor.getValue();
            assertThat(event.type()).isEqualTo(QuestEventType.QUEST_COMPLETED);
            assertThat(event.attributes())
                    .containsEntry("acceptanceId", ACCEPTANCE_ID)
                    .containsEntry("goalReachedAt", GOAL_REACHED_AT)
                    .containsEntry(
                            "completionPolicy",
                            QuestCompletionPolicy.USER_CONFIRM.name()
                    )
                    .containsEntry("questDefinitionVersion", 7)
                    .containsEntry("rewardProfileCode", "RP_EXP_30")
                    .doesNotContainKeys(
                            "rewardExp",
                            "rewardStats",
                            "rewardLines",
                            "rewardProfileId"
                    );
            assertThat(event.correlationId())
                    .isEqualTo("quest:193:acceptance:1930:completed");
            assertThat(event.occurredAt()).isEqualTo(result.completedAt());
        }

        @Test
        @DisplayName("동일 완료 재호출은 저장과 Event 발행을 반복하지 않는다")
        void isIdempotent() {
            Quest quest = quest(QuestCompletionPolicy.USER_CONFIRM);
            QuestAcceptance acceptance = goalReachedAcceptance(quest);
            given(questReader.getAcceptanceForUpdate(ACCEPTANCE_ID))
                    .willReturn(acceptance);
            given(questReader.getById(QUEST_ID)).willReturn(quest);

            QuestResult.Acceptance first = service.complete(ACCEPTANCE_ID);
            QuestResult.Acceptance replay = service.complete(ACCEPTANCE_ID);

            assertThat(replay.status()).isEqualTo(QuestStatus.COMPLETED.name());
            assertThat(replay.completedAt()).isEqualTo(first.completedAt());
            verify(questWriter, times(1)).saveAcceptance(acceptance);
            ArgumentCaptor<DomainEvent> eventCaptor =
                    ArgumentCaptor.forClass(DomainEvent.class);
            verify(domainEventPublisher, times(1))
                    .publish(eventCaptor.capture());
            QuestEvent event = (QuestEvent) eventCaptor.getValue();
            assertThat(event.attributes())
                    .containsEntry("questDefinitionVersion", 1)
                    .containsEntry("rewardExp", 25)
                    .containsEntry(
                            "rewardStats",
                            java.util.Map.of("vitality", 1)
                    )
                    .doesNotContainKeys(
                            "rewardProfileCode",
                            "rewardLines",
                            "rewardProfileId"
                    );
            assertThat(event.correlationId())
                    .isEqualTo("quest:193:acceptance:1930:completed");
        }
    }

    @Nested
    @DisplayName("명시적 완료 조건을 충족하지 못할 때")
    class RejectInvalidCompletion {

        @Test
        @DisplayName("AUTO 정책은 명시적 Completion Use Case로 완료하지 않는다")
        void rejectsAutoPolicy() {
            Quest quest = quest(QuestCompletionPolicy.AUTO);
            QuestAcceptance acceptance = goalReachedAcceptance(quest);
            given(questReader.getAcceptanceForUpdate(ACCEPTANCE_ID))
                    .willReturn(acceptance);
            given(questReader.getById(QUEST_ID)).willReturn(quest);

            assertQuestError(
                    () -> service.complete(ACCEPTANCE_ID),
                    QuestError.QUEST_COMPLETION_POLICY_NOT_USER_CONFIRM
            );
            verifyNoInteractions(questWriter, domainEventPublisher);
        }

        @Test
        @DisplayName("IN_PROGRESS 상태는 직접 완료할 수 없다")
        void rejectsInProgressAcceptance() {
            Quest quest = quest(QuestCompletionPolicy.USER_CONFIRM);
            QuestAcceptance acceptance = QuestAcceptance.start(
                    QUEST_ID,
                    10L,
                    TimePeriod.forever(),
                    ACCEPTED_AT,
                    null
            );
            ReflectionTestUtils.setField(acceptance, "id", ACCEPTANCE_ID);
            given(questReader.getAcceptanceForUpdate(ACCEPTANCE_ID))
                    .willReturn(acceptance);
            given(questReader.getById(QUEST_ID)).willReturn(quest);

            assertQuestError(
                    () -> service.complete(ACCEPTANCE_ID),
                    QuestError.QUEST_ACCEPTANCE_COMPLETION_NOT_ALLOWED
            );
            verifyNoInteractions(questWriter, domainEventPublisher);
        }

        @Test
        @DisplayName("Public 완료는 다른 Player Acceptance를 not found로 숨긴다")
        void rejectsOtherPlayer() {
            Quest quest = quest(QuestCompletionPolicy.USER_CONFIRM);
            QuestAcceptance acceptance = goalReachedAcceptance(quest);
            given(questReader.getAcceptanceForUpdate(ACCEPTANCE_ID))
                    .willReturn(acceptance);

            assertQuestError(
                    () -> service.completeForPlayer(99L, ACCEPTANCE_ID),
                    QuestError.QUEST_ACCEPTANCE_NOT_FOUND
            );

            verify(questReader, never()).getById(anyLong());
            verifyNoInteractions(questWriter, domainEventPublisher);
        }
    }

    private QuestAcceptance goalReachedAcceptance(Quest quest) {
        QuestAcceptance acceptance = QuestAcceptance.start(
                quest.getId(),
                10L,
                TimePeriod.forever(),
                ACCEPTED_AT,
                null
        );
        ReflectionTestUtils.setField(acceptance, "id", ACCEPTANCE_ID);
        acceptance.setProgress(1, quest, GOAL_REACHED_AT);
        return acceptance;
    }

    private Quest quest(QuestCompletionPolicy completionPolicy) {
        Quest quest = Quest.create(
                "quest:test:explicit-completion",
                QuestCategory.MAIN,
                QuestTitle.of("명시적 완료 테스트"),
                "Quest 명시적 완료 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                QuestReward.of(
                        25,
                        new RewardStats(java.util.Map.of("vitality", 1))
                ),
                QuestRepeatRule.NONE,
                completionPolicy,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }

    private Quest profileQuest(QuestCompletionPolicy completionPolicy) {
        Quest quest = Quest.createDefinition(
                "quest:test:explicit-completion-profile",
                7,
                QuestCategory.MAIN,
                QuestTitle.of("Profile 명시적 완료 테스트"),
                "RewardProfile Quest 명시적 완료 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 1),
                RewardProfileRef.of("RP_EXP_30"),
                QuestRepeatRule.NONE,
                completionPolicy,
                null
        );
        ReflectionTestUtils.setField(quest, "id", QUEST_ID);
        return quest;
    }

    private void assertQuestError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
