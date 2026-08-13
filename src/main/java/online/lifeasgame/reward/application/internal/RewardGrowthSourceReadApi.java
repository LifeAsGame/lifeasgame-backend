package online.lifeasgame.reward.application.internal;

import java.util.List;
import java.util.Set;

public interface RewardGrowthSourceReadApi {

    List<RewardGrowthSource> findAllByRewardLineIds(Set<Long> rewardLineIds);

    record RewardGrowthSource(Long rewardLineId, String sourceType, Long sourceId) {
    }
}
