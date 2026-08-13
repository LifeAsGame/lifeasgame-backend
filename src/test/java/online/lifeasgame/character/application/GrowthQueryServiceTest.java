package online.lifeasgame.character.application;

import online.lifeasgame.character.application.query.GrowthQuery;
import online.lifeasgame.character.application.result.GrowthResult;
import online.lifeasgame.character.domain.CoreStatDelta;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.reward.application.internal.RewardGrowthSourceReadApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Growth query composition")
class GrowthQueryServiceTest {

    private static final Long PLAYER_ID = 264L;
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-13T00:00:00Z");

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @Mock
    private PlayerReader playerReader;

    @Mock
    private GrowthQuery growthQuery;

    @Mock
    private RewardGrowthSourceReadApi rewardGrowthSourceReadApi;

    @Captor
    private ArgumentCaptor<Set<Long>> rewardLineIdsCaptor;

    @InjectMocks
    private GrowthQueryService service;

    @Nested
    @DisplayName("현재 성장 상태를 조회할 때")
    class CurrentGrowth {

        @Test
        @DisplayName("Player의 authoritative EXP와 stats를 그대로 반환한다")
        void returnsExactCurrentSnapshot() {
            Player player = player();
            player.grantCoreStats(new CoreStatDelta(1, 2, 3, 4, 5, 6));
            player.changeRepresentativeTitle(77L);
            givenCurrentPlayer(player);
            given(growthQuery.findRecentExpChanges(PLAYER_ID, 20)).willReturn(List.of());

            GrowthResult.Current current = service.getCurrentGrowth().current();

            assertThat(current).isEqualTo(new GrowthResult.Current(
                    1, 0, 2, 3, 4, 5, 6, 7, Map.of(), 77L
            ));
        }

        @Test
        @DisplayName("history가 없으면 빈 목록이며 Reward provider를 호출하지 않는다")
        void returnsEmptyHistoryWithoutProviderQuery() {
            givenCurrentPlayer(player());
            given(growthQuery.findRecentExpChanges(PLAYER_ID, 20)).willReturn(List.of());

            assertThat(service.getCurrentGrowth().recentExpChanges()).isEmpty();

            verifyNoInteractions(rewardGrowthSourceReadApi);
        }
    }

    @Nested
    @DisplayName("최근 EXP 변경을 조합할 때")
    class RecentExpChanges {

        @Test
        @DisplayName("SQL query boundary에 고정 limit 20을 전달한다")
        void requestsFixedSqlLimit() {
            givenCurrentPlayer(player());
            given(growthQuery.findRecentExpChanges(PLAYER_ID, 20)).willReturn(List.of());

            service.getCurrentGrowth();

            verify(growthQuery).findRecentExpChanges(PLAYER_ID, 20);
        }

        @Test
        @DisplayName("중복을 제거한 rewardLineId를 한 번에 조회하고 provenance를 붙인다")
        void resolvesUniqueRewardLinesOnce() {
            givenCurrentPlayer(player());
            List<GrowthQuery.RecentExpChange> changes = List.of(
                    change(2L, 22L),
                    change(1L, 22L)
            );
            given(growthQuery.findRecentExpChanges(PLAYER_ID, 20)).willReturn(changes);
            given(rewardGrowthSourceReadApi.findAllByRewardLineIds(Set.of(22L)))
                    .willReturn(List.of(new RewardGrowthSourceReadApi.RewardGrowthSource(
                            22L, "QUEST_COMPLETION", 222L
                    )));

            List<GrowthResult.RecentExpChange> result = service
                    .getCurrentGrowth().recentExpChanges();

            verify(rewardGrowthSourceReadApi)
                    .findAllByRewardLineIds(rewardLineIdsCaptor.capture());
            assertThat(rewardLineIdsCaptor.getValue()).containsExactly(22L);
            assertThat(result).extracting(GrowthResult.RecentExpChange::changeId)
                    .containsExactly(2L, 1L);
            assertThat(result).allSatisfy(change -> {
                assertThat(change.sourceType()).isEqualTo("QUEST_COMPLETION");
                assertThat(change.sourceId()).isEqualTo(222L);
            });
        }

        @Test
        @DisplayName("Reward source가 없어도 Growth row를 null provenance로 보존한다")
        void keepsGrowthWhenSourceIsMissing() {
            givenCurrentPlayer(player());
            given(growthQuery.findRecentExpChanges(PLAYER_ID, 20))
                    .willReturn(List.of(change(1L, 11L)));
            given(rewardGrowthSourceReadApi.findAllByRewardLineIds(Set.of(11L)))
                    .willReturn(List.of());

            GrowthResult.RecentExpChange result = service
                    .getCurrentGrowth().recentExpChanges().getFirst();

            assertThat(result.changeId()).isEqualTo(1L);
            assertThat(result.sourceType()).isNull();
            assertThat(result.sourceId()).isNull();
        }
    }

    private void givenCurrentPlayer(Player player) {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
        given(playerReader.getByIdOrThrow(PLAYER_ID)).willReturn(player);
    }

    private Player player() {
        return Player.linkStart(1L, Name.of("player"), GenderType.MALE);
    }

    private GrowthQuery.RecentExpChange change(Long changeId, Long rewardLineId) {
        return new GrowthQuery.RecentExpChange(
                changeId,
                rewardLineId,
                10,
                10,
                0,
                1,
                1,
                0,
                10,
                OCCURRED_AT
        );
    }
}
