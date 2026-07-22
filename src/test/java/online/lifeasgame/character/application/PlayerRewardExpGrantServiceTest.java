package online.lifeasgame.character.application;

import online.lifeasgame.character.application.growth.PlayerGrowthChangeReader;
import online.lifeasgame.character.application.growth.PlayerGrowthChangeWriter;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.growth.PlayerGrowthChange;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerRewardExpGrantService")
class PlayerRewardExpGrantServiceTest {

    @Mock
    private PlayerReader playerReader;

    @Mock
    private PlayerExpGrantService playerExpGrantService;

    @Mock
    private PlayerGrowthChangeReader growthChangeReader;

    @Mock
    private PlayerGrowthChangeWriter growthChangeWriter;

    private PlayerRewardExpGrantService service;

    @BeforeEach
    void setUp() {
        service = new PlayerRewardExpGrantService(
                playerReader,
                playerExpGrantService,
                growthChangeReader,
                growthChangeWriter
        );
    }

    @Nested
    @DisplayName("Reward EXP를 최초 지급할 때")
    class GrantFirstTime {

        @Test
        @DisplayName("Player lock 후 ledger를 확인하고 EXP와 GrowthChange를 함께 반영한다")
        void locksPlayerAndCreatesGrowthChange() {
            Player player = player();
            Player.GainResult gainResult = gainResult();
            given(playerReader.getByIdForUpdateOrThrow(1L)).willReturn(player);
            given(growthChangeReader.findByRewardLineId(10L)).willReturn(Optional.empty());
            given(playerExpGrantService.grantExp(player, 30L)).willReturn(gainResult);
            given(growthChangeWriter.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                    .willAnswer(invocation -> {
                        PlayerGrowthChange change = invocation.getArgument(0);
                        ReflectionTestUtils.setField(change, "id", 100L);
                        return change;
                    });

            var result = service.grantRewardExp(1L, 10L, 30L);

            assertThat(result.growthChangeId()).isEqualTo(100L);
            assertThat(result.rewardLineId()).isEqualTo(10L);
            assertThat(result.appliedExp()).isEqualTo(30L);
            assertThat(result.beforeTotalExp()).isZero();
            assertThat(result.afterTotalExp()).isEqualTo(30L);
            assertThat(result.replayed()).isFalse();

            InOrder order = inOrder(playerReader, growthChangeReader, playerExpGrantService, growthChangeWriter);
            order.verify(playerReader).getByIdForUpdateOrThrow(1L);
            order.verify(growthChangeReader).findByRewardLineId(10L);
            order.verify(playerExpGrantService).grantExp(player, 30L);
            order.verify(growthChangeWriter).saveAndFlush(org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("같은 rewardLineId를 replay할 때")
    class ReplayRewardLine {

        @Test
        @DisplayName("기존 GrowthChange 결과를 반환하고 EXP와 이벤트 경로를 다시 호출하지 않는다")
        void returnsExistingGrowthChange() {
            Player player = player();
            PlayerGrowthChange existing = PlayerGrowthChange.rewardExp(1L, 10L, gainResult());
            ReflectionTestUtils.setField(existing, "id", 100L);
            given(playerReader.getByIdForUpdateOrThrow(1L)).willReturn(player);
            given(growthChangeReader.findByRewardLineId(10L)).willReturn(Optional.of(existing));

            var result = service.grantRewardExp(1L, 10L, 30L);

            assertThat(result.growthChangeId()).isEqualTo(100L);
            assertThat(result.replayed()).isTrue();
            verify(playerExpGrantService, never()).grantExp(player, 30L);
            verify(growthChangeWriter, never()).saveAndFlush(existing);
        }

        @Test
        @DisplayName("기존 GrowthChange의 Player나 요청량이 다르면 invariant error로 중단한다")
        void rejectsInconsistentReplay() {
            Player player = player();
            PlayerGrowthChange existing = PlayerGrowthChange.rewardExp(1L, 10L, gainResult());
            given(playerReader.getByIdForUpdateOrThrow(1L)).willReturn(player);
            given(growthChangeReader.findByRewardLineId(10L)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.grantRewardExp(1L, 10L, 20L))
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PlayerError.PLAYER_GROWTH_CHANGE_INCONSISTENT)
                    );
            verify(playerExpGrantService, never()).grantExp(player, 20L);
        }
    }

    private Player player() {
        Player player = Player.linkStart(10L, Name.of("테스터"), GenderType.MALE);
        ReflectionTestUtils.setField(player, "id", 1L);
        return player;
    }

    private Player.GainResult gainResult() {
        return new Player.GainResult(
                30L, 30L, 0L, 1, 1, 0L, 30L, 30L, 70L, 100L, 0.3
        );
    }
}
