package online.lifeasgame.reward.application.result;

import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementStatus;

public record RewardSettlementItemProcessResult(
        Long settlementId,
        Long lineId,
        Long playerId,
        RewardSettlementLineStatus lineStatus,
        RewardSettlementStatus settlementStatus,
        Long itemId,
        String itemCode,
        long quantity,
        boolean replayed,
        Long deliveryId
) {
}
