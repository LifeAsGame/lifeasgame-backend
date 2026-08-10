package online.lifeasgame.quest.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestRoute 정의")
class QuestRouteTest {

    @Nested
    @DisplayName("순서가 있는 Step으로 Route를 정의할 때")
    class DefineRoute {

        @Test
        @DisplayName("첫 Step과 Quest completion-set 계약을 보존한다")
        void preservesOrderedStepsAndQuestLinks() {
            QuestRouteStep first = step(1, 101L);
            QuestRouteStep second = step(2, 102L);

            QuestRoute route = QuestRoute.define(
                    "ROUTE_RECORD_START",
                    1,
                    "기록으로 시작하기",
                    "기록을 남기고 연결하는 방향",
                    null,
                    List.of(first, second)
            );

            assertThat(route.firstStep()).isSameAs(first);
            assertThat(route.getSteps()).containsExactly(first, second);
            assertThat(first.requiredQuestIds()).containsExactly(101L);
            assertThat(first.getCriterionType())
                    .isEqualTo(QuestRouteCriterionType.QUEST_COMPLETION_SET);
            assertThat(first.isUserAdvanceRequired()).isTrue();
            assertThat(first.isRetroactiveEvidenceAllowed()).isTrue();
            assertThat(first.isSkipAllowed()).isFalse();
        }

        @Test
        @DisplayName("Step order가 1부터 연속되지 않으면 정의를 거부한다")
        void rejectsNonContiguousStepOrder() {
            assertInvalidDefinition(() -> QuestRoute.define(
                    "ROUTE_RECORD_START",
                    1,
                    "기록으로 시작하기",
                    null,
                    null,
                    List.of(step(1, 101L), step(3, 102L))
            ));
        }

        @Test
        @DisplayName("같은 Step code가 중복되면 정의를 거부한다")
        void rejectsDuplicateStepCode() {
            QuestRouteStep first = QuestRouteStep.define(
                    "SAME_STEP",
                    1,
                    "첫 단계",
                    null,
                    1,
                    Set.of(QuestRouteStepQuest.required(101L))
            );
            QuestRouteStep second = QuestRouteStep.define(
                    "SAME_STEP",
                    2,
                    "둘째 단계",
                    null,
                    1,
                    Set.of(QuestRouteStepQuest.required(102L))
            );

            assertInvalidDefinition(() -> QuestRoute.define(
                    "ROUTE_RECORD_START",
                    1,
                    "기록으로 시작하기",
                    null,
                    null,
                    List.of(first, second)
            ));
        }
    }

    @Nested
    @DisplayName("Step의 Quest evidence 계약을 정의할 때")
    class DefineStepCriteria {

        @Test
        @DisplayName("Optional Quest는 required evidence 수에 포함하지 않는다")
        void excludesOptionalQuestFromRequiredEvidence() {
            QuestRouteStep step = QuestRouteStep.define(
                    "STEP_ONE",
                    1,
                    "첫 단계",
                    null,
                    1,
                    Set.of(
                            QuestRouteStepQuest.required(101L),
                            QuestRouteStepQuest.optional(102L)
                    )
            );

            assertThat(step.requiredQuestIds()).containsExactly(101L);
        }

        @Test
        @DisplayName("required Quest보다 많은 evidence를 요구하면 정의를 거부한다")
        void rejectsImpossibleRequiredEvidenceCount() {
            assertInvalidDefinition(() -> QuestRouteStep.define(
                    "STEP_ONE",
                    1,
                    "첫 단계",
                    null,
                    2,
                    Set.of(
                            QuestRouteStepQuest.required(101L),
                            QuestRouteStepQuest.optional(102L)
                    )
            ));
        }

        @Test
        @DisplayName("Quest ID가 유효하지 않으면 link 생성을 거부한다")
        void rejectsInvalidQuestLink() {
            assertInvalidDefinition(() -> QuestRouteStepQuest.required(0L));
        }
    }

    private QuestRouteStep step(int order, Long questId) {
        return QuestRouteStep.define(
                "STEP_" + order,
                order,
                "단계 " + order,
                null,
                1,
                Set.of(QuestRouteStepQuest.required(questId))
        );
    }

    private void assertInvalidDefinition(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(QuestError.ROUTE_DEFINITION_INVALID)
                );
    }
}
