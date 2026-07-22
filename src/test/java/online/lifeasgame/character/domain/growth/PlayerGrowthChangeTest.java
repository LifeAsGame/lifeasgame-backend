package online.lifeasgame.character.domain.growth;

import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlayerGrowthChange")
class PlayerGrowthChangeTest {

    @Nested
    @DisplayName("Reward EXP 성장 변경을 생성할 때")
    class CreateRewardGrowthChange {

        @Test
        @DisplayName("Player와 rewardLine 및 EXP 적용 결과를 스냅샷으로 보존한다")
        void createsSnapshot() {
            PlayerGrowthChange change = PlayerGrowthChange.rewardExp(1L, 10L, validResult());

            assertThat(change.getPlayerId()).isEqualTo(1L);
            assertThat(change.getRewardLineId()).isEqualTo(10L);
            assertThat(change.getRequestedExp()).isEqualTo(30L);
            assertThat(change.getAppliedExp()).isEqualTo(30L);
            assertThat(change.getBeforeTotalExp()).isZero();
            assertThat(change.getAfterTotalExp()).isEqualTo(30L);
        }

        @Test
        @DisplayName("rewardLineId가 없거나 양수가 아니면 PlayerError로 거부한다")
        void requiresPositiveRewardLineId() {
            assertPlayerError(
                    () -> PlayerGrowthChange.rewardExp(1L, 0L, validResult()),
                    PlayerError.PLAYER_GROWTH_REWARD_LINE_ID_REQUIRED
            );
        }

        @Test
        @DisplayName("지급량 합계가 맞지 않으면 안정된 PlayerError로 거부한다")
        void rejectsInvalidAmountConsistency() {
            Player.GainResult invalid = new Player.GainResult(
                    30L, 20L, 5L, 1, 1, 0L, 20L, 20L, 80L, 100L, 0.2
            );

            assertPlayerError(
                    () -> PlayerGrowthChange.rewardExp(1L, 10L, invalid),
                    PlayerError.PLAYER_GROWTH_CHANGE_INVALID
            );
        }

        @Test
        @DisplayName("레벨 또는 총 EXP 정합성이 맞지 않으면 안정된 PlayerError로 거부한다")
        void rejectsInvalidLevelAndTotalConsistency() {
            Player.GainResult invalid = new Player.GainResult(
                    30L, 30L, 0L, 2, 1, 0L, 20L, 20L, 80L, 100L, 0.2
            );

            assertPlayerError(
                    () -> PlayerGrowthChange.rewardExp(1L, 10L, invalid),
                    PlayerError.PLAYER_GROWTH_CHANGE_INVALID
            );
        }
    }

    @Nested
    @DisplayName("기존 GrowthChange를 replay할 때")
    class ValidateReplayIdentity {

        @Test
        @DisplayName("Player, rewardLine, 요청 EXP가 다르면 invariant error로 중단한다")
        void rejectsDifferentIdentity() {
            PlayerGrowthChange change = PlayerGrowthChange.rewardExp(1L, 10L, validResult());

            assertPlayerError(
                    () -> change.assertMatches(2L, 10L, 30L),
                    PlayerError.PLAYER_GROWTH_CHANGE_INCONSISTENT
            );
        }
    }

    private Player.GainResult validResult() {
        return new Player.GainResult(
                30L, 30L, 0L, 1, 1, 0L, 30L, 30L, 70L, 100L, 0.3
        );
    }

    private void assertPlayerError(Runnable action, PlayerError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
