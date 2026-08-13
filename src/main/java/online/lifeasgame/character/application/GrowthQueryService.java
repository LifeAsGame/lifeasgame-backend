package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.GrowthQuery;
import online.lifeasgame.character.application.result.GrowthResult;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.reward.application.internal.RewardGrowthSourceReadApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrowthQueryService {

    static final int RECENT_EXP_CHANGE_LIMIT = 20;

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerReader playerReader;
    private final GrowthQuery growthQuery;
    private final RewardGrowthSourceReadApi rewardGrowthSourceReadApi;

    public GrowthResult.Overview getCurrentGrowth() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Player player = playerReader.getByIdOrThrow(playerId);
        List<GrowthQuery.RecentExpChange> changes = growthQuery.findRecentExpChanges(
                playerId,
                RECENT_EXP_CHANGE_LIMIT
        );
        Map<Long, RewardGrowthSourceReadApi.RewardGrowthSource> sources = sources(changes);

        return new GrowthResult.Overview(
                GrowthResult.Current.from(player),
                changes.stream()
                        .map(change -> toResult(change, sources.get(change.rewardLineId())))
                        .toList()
        );
    }

    private Map<Long, RewardGrowthSourceReadApi.RewardGrowthSource> sources(
            List<GrowthQuery.RecentExpChange> changes
    ) {
        if (changes.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<Long> rewardLineIds = changes.stream()
                .map(GrowthQuery.RecentExpChange::rewardLineId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return rewardGrowthSourceReadApi.findAllByRewardLineIds(rewardLineIds).stream()
                .collect(Collectors.toMap(
                        RewardGrowthSourceReadApi.RewardGrowthSource::rewardLineId,
                        Function.identity()
                ));
    }

    private GrowthResult.RecentExpChange toResult(
            GrowthQuery.RecentExpChange change,
            RewardGrowthSourceReadApi.RewardGrowthSource source
    ) {
        return new GrowthResult.RecentExpChange(
                change.changeId(),
                change.requestedExp(),
                change.appliedExp(),
                change.leftoverExp(),
                change.beforeLevel(),
                change.afterLevel(),
                change.beforeTotalExp(),
                change.afterTotalExp(),
                change.occurredAt(),
                source == null ? null : source.sourceType(),
                source == null ? null : source.sourceId()
        );
    }
}
