package online.lifeasgame.character.application.internal;

import java.util.Optional;

public interface PlayerGrowthApi {

    PlayerGrowthGrantResult grantRewardExp(Long playerId, Long rewardLineId, long amount);

    Optional<PlayerGrowthGrantResult> findRewardExpGrant(Long rewardLineId);

    record PlayerGrowthGrantResult(
            Long growthChangeId,
            Long playerId,
            Long rewardLineId,
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int beforeLevel,
            int afterLevel,
            long beforeTotalExp,
            long afterTotalExp,
            boolean replayed
    ) {
    }
}
