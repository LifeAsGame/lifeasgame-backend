package online.lifeasgame.character.domain;

import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.event.PlayerLeveledUp;
import online.lifeasgame.character.domain.service.PrecomputedLevelingPolicy;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Player EXP 도메인")
class PlayerExperienceTest {

    @Nested
    @DisplayName("EXP 값을 검증할 때")
    class ValidateExperience {

        @Test
        @DisplayName("음수 총 EXP는 PlayerError 기반 DomainException으로 거부한다")
        void rejectsNegativeTotalExp() {
            assertPlayerError(() -> Experience.of(-1L), PlayerError.PLAYER_EXP_MUST_NOT_BE_NEGATIVE);
        }

        @Test
        @DisplayName("음수 증가량은 PlayerError 기반 DomainException으로 거부한다")
        void rejectsNegativeDelta() {
            Experience experience = Experience.of(10L);

            assertPlayerError(() -> experience.plus(-1L), PlayerError.PLAYER_EXP_MUST_NOT_BE_NEGATIVE);
        }

        @Test
        @DisplayName("총 EXP 오버플로는 PlayerError 기반 DomainException으로 변환한다")
        void rejectsOverflow() {
            Experience experience = Experience.of(Long.MAX_VALUE);

            assertPlayerError(() -> experience.plus(1L), PlayerError.PLAYER_EXP_OVERFLOW);
        }
    }

    @Nested
    @DisplayName("Player에게 EXP를 지급할 때")
    class GainExperience {

        @Test
        @DisplayName("0 이하 지급량을 PlayerError로 거부한다")
        void rejectsNonPositiveAmount() {
            Player player = player();

            assertPlayerError(
                    () -> player.gainExp(0L, levelingPolicy()),
                    PlayerError.PLAYER_EXP_AMOUNT_MUST_BE_POSITIVE
            );
        }

        @Test
        @DisplayName("LevelingPolicy가 없으면 PlayerError로 거부한다")
        void requiresLevelingPolicy() {
            Player player = player();

            assertPlayerError(
                    () -> player.gainExp(10L, null),
                    PlayerError.PLAYER_LEVELING_POLICY_REQUIRED
            );
        }

        @Test
        @DisplayName("레벨 경계를 넘으면 결과와 기존 PlayerLeveledUp 이벤트를 생성한다")
        void createsLevelUpResultAndEvent() {
            Player player = player();

            Player.GainResult result = player.gainExp(100L, levelingPolicy());

            assertThat(result.beforeLevel()).isEqualTo(1);
            assertThat(result.afterLevel()).isEqualTo(2);
            assertThat(result.appliedExp()).isEqualTo(100L);
            assertThat(result.totalExp()).isEqualTo(100L);
            assertThat(player.pullEvents())
                    .singleElement()
                    .isInstanceOfSatisfying(PlayerLeveledUp.class, event -> {
                        assertThat(event.playerId()).isEqualTo(1L);
                        assertThat(event.beforeLevel()).isEqualTo(1);
                        assertThat(event.afterLevel()).isEqualTo(2);
                    });
        }

        @Test
        @DisplayName("max level 상한까지만 적용하고 남은 EXP를 반환한다")
        void capsAtMaxLevelAndReturnsLeftover() {
            Player player = player();

            Player.GainResult result = player.gainExp(300L, levelingPolicy());

            assertThat(result.appliedExp()).isEqualTo(201L);
            assertThat(result.leftoverExp()).isEqualTo(99L);
            assertThat(result.afterLevel()).isEqualTo(3);
            assertThat(result.totalExp()).isEqualTo(201L);
        }
    }

    private Player player() {
        Player player = Player.linkStart(10L, Name.of("테스터"), GenderType.MALE);
        ReflectionTestUtils.setField(player, "id", 1L);
        return player;
    }

    private PrecomputedLevelingPolicy levelingPolicy() {
        return new PrecomputedLevelingPolicy(new LevelingPolicyParameters(
                3,
                100L,
                java.util.List.of(new LevelingPolicyParameters.Bracket(1, 3, 1.0, 0L))
        ));
    }

    private void assertPlayerError(Runnable action, PlayerError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
