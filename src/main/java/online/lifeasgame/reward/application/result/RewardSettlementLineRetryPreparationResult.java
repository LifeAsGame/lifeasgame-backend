package online.lifeasgame.reward.application.result;

import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementStatus;

public record RewardSettlementLineRetryPreparationResult(
        Long settlementId,
        Long lineId,
        RewardSettlementLineStatus lineStatus,
        RewardSettlementStatus settlementStatus,
        boolean changed
) {
}
