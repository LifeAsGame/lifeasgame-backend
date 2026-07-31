package online.lifeasgame.quest.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestAcceptance")
class QuestAcceptanceTest {

    private static final Instant GOAL_REACHED_AT = Instant.parse("2026-07-23T01:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-07-23T01:01:00Z");
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-23T00:00:00Z");

    @Nested
    @DisplayName("Fact context로 시작할 때")
    class StartWithFactContext {

        @Test
        @DisplayName("acceptedAt과 유효한 nullable periodKey를 명시적으로 저장한다")
        void storesAcceptedAtAndPeriodKey() {
            QuestAcceptance acceptance = QuestAcceptance.start(
                    1L,
                    10L,
                    TimePeriod.weekly(java.time.LocalDate.of(2026, 7, 30)),
                    ACCEPTED_AT,
                    "2026-W31"
            );

            assertThat(acceptance.getAcceptedAt()).isEqualTo(ACCEPTED_AT);
            assertThat(acceptance.getPeriodKey()).isEqualTo("2026-W31");
        }

        @Test
        @DisplayName("acceptedAt null과 잘못된 weekly periodKey를 거부한다")
        void rejectsInvalidFactContext() {
            assertThatThrownBy(() -> QuestAcceptance.start(
                    1L,
                    10L,
                    TimePeriod.forever(),
                    null,
                    null
            )).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("acceptedAt");
            assertThatThrownBy(() -> QuestAcceptance.start(
                    1L,
                    10L,
                    TimePeriod.forever(),
                    ACCEPTED_AT,
                    "2026-W54"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey");
        }
    }

    @Nested
    @DisplayName("진행도를 반영할 때")
    class UpdateProgress {

        @Test
        @DisplayName("새 Acceptance는 IN_PROGRESS이고 목표 미달이면 상태를 유지한다")
        void remainsInProgressBelowTarget() {
            Quest quest = quest(QuestCompletionPolicy.AUTO);
            QuestAcceptance acceptance = acceptance();

            acceptance.addProgress(2, quest, GOAL_REACHED_AT);

            assertThat(acceptance.getStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
            assertThat(acceptance.getProgressValue()).isEqualTo(2);
            assertThat(acceptance.getGoalReachedAt()).isNull();
            assertThat(acceptance.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("목표를 충족하면 GOAL_REACHED로 전이하고 최초 시각을 기록한다")
        void reachesGoalAtTarget() {
            Quest quest = quest(QuestCompletionPolicy.USER_CONFIRM);
            QuestAcceptance acceptance = acceptance();

            acceptance.setProgress(3, quest, GOAL_REACHED_AT);

            assertThat(acceptance.getStatus()).isEqualTo(QuestStatus.GOAL_REACHED);
            assertThat(acceptance.getGoalReachedAt()).isEqualTo(GOAL_REACHED_AT);
            assertThat(acceptance.getCompletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("목표 도달 상태를 처리할 때")
    class ReachGoal {

        @Test
        @DisplayName("GOAL_REACHED 재호출은 no-op이고 최초 timestamp를 유지한다")
        void isIdempotent() {
            QuestAcceptance acceptance = acceptance();
            acceptance.reachGoal(GOAL_REACHED_AT);

            boolean changed = acceptance.reachGoal(GOAL_REACHED_AT.plusSeconds(60));

            assertThat(changed).isFalse();
            assertThat(acceptance.getGoalReachedAt()).isEqualTo(GOAL_REACHED_AT);
        }

        @Test
        @DisplayName("취소된 Acceptance는 다시 목표 도달 상태로 전이할 수 없다")
        void rejectsCanceledAcceptance() {
            QuestAcceptance acceptance = acceptance();
            acceptance.cancel();

            assertQuestError(
                    () -> acceptance.reachGoal(GOAL_REACHED_AT),
                    QuestError.QUEST_ACCEPTANCE_GOAL_REACH_NOT_ALLOWED
            );
        }
    }

    @Nested
    @DisplayName("완료할 때")
    class Complete {

        @Test
        @DisplayName("AUTO 정책도 GOAL_REACHED를 거쳐 순차적으로 COMPLETED가 된다")
        void completesAutoQuestSequentially() {
            Quest quest = quest(QuestCompletionPolicy.AUTO);
            QuestAcceptance acceptance = acceptance();

            acceptance.setProgress(3, quest, GOAL_REACHED_AT);
            boolean changed = acceptance.complete(COMPLETED_AT);

            assertThat(changed).isTrue();
            assertThat(acceptance.getStatus()).isEqualTo(QuestStatus.COMPLETED);
            assertThat(acceptance.getGoalReachedAt()).isEqualTo(GOAL_REACHED_AT);
            assertThat(acceptance.getCompletedAt()).isEqualTo(COMPLETED_AT);
        }

        @Test
        @DisplayName("USER_CONFIRM 정책은 목표 도달 후 명시적 완료 전까지 대기한다")
        void waitsForExplicitCompletion() {
            Quest quest = quest(QuestCompletionPolicy.USER_CONFIRM);
            QuestAcceptance acceptance = acceptance();

            acceptance.setProgress(3, quest, GOAL_REACHED_AT);

            assertThat(quest.requiresUserConfirmation()).isTrue();
            assertThat(acceptance.getStatus()).isEqualTo(QuestStatus.GOAL_REACHED);
            assertThat(acceptance.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("COMPLETED 재호출은 no-op이고 최초 timestamp를 유지한다")
        void isIdempotent() {
            QuestAcceptance acceptance = goalReachedAcceptance();
            acceptance.complete(COMPLETED_AT);

            boolean changed = acceptance.complete(COMPLETED_AT.plusSeconds(60));

            assertThat(changed).isFalse();
            assertThat(acceptance.getCompletedAt()).isEqualTo(COMPLETED_AT);
        }

        @Test
        @DisplayName("IN_PROGRESS에서 직접 완료할 수 없다")
        void rejectsDirectCompletion() {
            QuestAcceptance acceptance = acceptance();

            assertQuestError(
                    () -> acceptance.complete(COMPLETED_AT),
                    QuestError.QUEST_ACCEPTANCE_COMPLETION_NOT_ALLOWED
            );
        }
    }

    @Nested
    @DisplayName("취소할 때")
    class Cancel {

        @Test
        @DisplayName("IN_PROGRESS와 GOAL_REACHED는 CANCELED로 전이할 수 있다")
        void cancelsActiveAcceptance() {
            QuestAcceptance inProgress = acceptance();
            QuestAcceptance goalReached = goalReachedAcceptance();

            assertThat(inProgress.cancel()).isTrue();
            assertThat(goalReached.cancel()).isTrue();

            assertThat(inProgress.getStatus()).isEqualTo(QuestStatus.CANCELED);
            assertThat(goalReached.getStatus()).isEqualTo(QuestStatus.CANCELED);
        }

        @Test
        @DisplayName("CANCELED 재호출은 no-op이다")
        void isIdempotent() {
            QuestAcceptance acceptance = acceptance();
            acceptance.cancel();

            assertThat(acceptance.cancel()).isFalse();
            assertThat(acceptance.getStatus()).isEqualTo(QuestStatus.CANCELED);
        }

        @Test
        @DisplayName("COMPLETED 이후 취소할 수 없다")
        void rejectsCompletedAcceptance() {
            QuestAcceptance acceptance = goalReachedAcceptance();
            acceptance.complete(COMPLETED_AT);

            assertQuestError(
                    acceptance::cancel,
                    QuestError.QUEST_ACCEPTANCE_CANCELLATION_NOT_ALLOWED
            );
        }

        @Test
        @DisplayName("CANCELED 이후 IN_PROGRESS로 재개할 수 없다")
        void rejectsResume() {
            QuestAcceptance acceptance = acceptance();
            acceptance.cancel();

            assertQuestError(
                    () -> acceptance.changeStatus(QuestStatus.IN_PROGRESS, COMPLETED_AT),
                    QuestError.QUEST_ACCEPTANCE_STATUS_TRANSITION_NOT_ALLOWED
            );
        }
    }

    private QuestAcceptance goalReachedAcceptance() {
        QuestAcceptance acceptance = acceptance();
        acceptance.reachGoal(GOAL_REACHED_AT);
        return acceptance;
    }

    private QuestAcceptance acceptance() {
        return QuestAcceptance.start(
                1L,
                10L,
                TimePeriod.forever(),
                ACCEPTED_AT,
                null
        );
    }

    private Quest quest(QuestCompletionPolicy completionPolicy) {
        return Quest.create(
                "quest:test:state-contract",
                QuestCategory.MAIN,
                QuestTitle.of("상태 계약 테스트"),
                "Quest 상태 전이 테스트",
                QuestTarget.of(QuestTargetType.COUNT, 3),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                completionPolicy,
                null
        );
    }

    private void assertQuestError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
