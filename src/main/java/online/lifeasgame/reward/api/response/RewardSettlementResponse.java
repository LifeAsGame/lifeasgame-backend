package online.lifeasgame.reward.api.response;

import java.time.Instant;
import java.util.List;

public final class RewardSettlementResponse {

    private RewardSettlementResponse() {
    }

    public record Detail(
            Long settlementId,
            String sourceType,
            Long sourceId,
            Long rewardProfileId,
            String rewardProfileCode,
            String status,
            Instant createdAt,
            Instant updatedAt,
            List<Line> lines
    ) {
    }

    public record Line(
            Long lineId,
            Long rewardDefinitionId,
            String rewardDefinitionCode,
            String rewardType,
            long amount,
            Long itemId,
            String itemCode,
            int sortOrder,
            String status,
            String failureCode,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
