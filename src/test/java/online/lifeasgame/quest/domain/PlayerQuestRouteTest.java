package online.lifeasgame.quest.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlayerQuestRoute 진행")
class PlayerQuestRouteTest {

    private static final Instant SELECTED_AT =
            Instant.parse("2026-08-10T01:00:00Z");

    @Nested
    @DisplayName("Route를 처음 선택하면")
    class StartRoute {

        @Test
        @DisplayName("첫 Step이 current인 IN_PROGRESS runtime을 만든다")
        void startsAtFirstStep() {
            PlayerQuestRoute playerRoute = PlayerQuestRoute.start(
                    1001L,
                    201L,
                    301L,
                    SELECTED_AT
            );

            assertThat(playerRoute.getPlayerId()).isEqualTo(1001L);
            assertThat(playerRoute.getRouteId()).isEqualTo(201L);
            assertThat(playerRoute.getCurrentStepId()).isEqualTo(301L);
            assertThat(playerRoute.getStatus())
                    .isEqualTo(PlayerQuestRouteStatus.IN_PROGRESS);
            assertThat(playerRoute.getSelectedAt()).isEqualTo(SELECTED_AT);
            assertThat(playerRoute.getCompletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("현재 Step을 명시적으로 진행하면")
    class AdvanceRoute {

        @Test
        @DisplayName("정확히 다음 한 Step으로 이동한다")
        void advancesOneStep() {
            PlayerQuestRoute playerRoute = playerRoute();

            playerRoute.advanceTo(301L, 302L);

            assertThat(playerRoute.getCurrentStepId()).isEqualTo(302L);
            assertThat(playerRoute.getStatus())
                    .isEqualTo(PlayerQuestRouteStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("stale expectedStepId는 다시 진행시키지 않는다")
        void rejectsStaleExpectedStep() {
            PlayerQuestRoute playerRoute = playerRoute();
            playerRoute.advanceTo(301L, 302L);

            assertError(
                    () -> playerRoute.advanceTo(301L, 303L),
                    QuestError.ROUTE_STEP_NOT_CURRENT
            );
            assertThat(playerRoute.getCurrentStepId()).isEqualTo(302L);
        }

        @Test
        @DisplayName("마지막 Step의 명시적 진행은 완료 시각을 기록한다")
        void completesFinalStep() {
            PlayerQuestRoute playerRoute = playerRoute();
            Instant completedAt = SELECTED_AT.plusSeconds(60);

            playerRoute.complete(301L, completedAt);

            assertThat(playerRoute.getStatus())
                    .isEqualTo(PlayerQuestRouteStatus.COMPLETED);
            assertThat(playerRoute.getCompletedAt()).isEqualTo(completedAt);
            assertThat(playerRoute.getCurrentStepId()).isEqualTo(301L);
        }

        @Test
        @DisplayName("완료된 Route는 다시 진행하지 않는다")
        void rejectsAdvanceAfterCompletion() {
            PlayerQuestRoute playerRoute = playerRoute();
            playerRoute.complete(301L, SELECTED_AT.plusSeconds(60));

            assertError(
                    () -> playerRoute.complete(
                            301L,
                            SELECTED_AT.plusSeconds(120)
                    ),
                    QuestError.ROUTE_ALREADY_COMPLETED
            );
        }
    }

    private PlayerQuestRoute playerRoute() {
        return PlayerQuestRoute.start(1001L, 201L, 301L, SELECTED_AT);
    }

    private void assertError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
