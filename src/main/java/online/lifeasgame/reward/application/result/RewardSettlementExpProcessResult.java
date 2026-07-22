package online.lifeasgame.reward.application.result;

import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementStatus;

public record RewardSettlementExpProcessResult(
        Long settlementId,
        Long lineId,
        Long playerId,
        RewardSettlementLineStatus lineStatus,
        RewardSettlementStatus settlementStatus,
        long requestedExp,
        long appliedExp,
        long leftoverExp,
        int beforeLevel,
        int afterLevel,
        long beforeTotalExp,
        long afterTotalExp,
        boolean replayed,
        Long growthChangeId
) {
}
