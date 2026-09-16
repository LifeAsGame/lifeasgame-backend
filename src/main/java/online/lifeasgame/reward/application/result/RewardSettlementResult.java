package online.lifeasgame.reward.application.result;

import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import online.lifeasgame.reward.domain.RewardType;

import java.time.Instant;
import java.util.List;

public final class RewardSettlementResult {

    private RewardSettlementResult() {
    }

    public record Detail(
            Long settlementId,
            RewardSettlementSourceType sourceType,
            Long sourceId,
            Long rewardProfileId,
            String rewardProfileCode,
            RewardSettlementStatus status,
            Instant createdAt,
            Instant updatedAt,
            List<Line> lines
    ) {
        public Detail {
            lines = List.copyOf(lines);
        }

        public static Detail from(RewardSettlement settlement) {
            return new Detail(
                    settlement.getId(),
                    settlement.getSourceType(),
                    settlement.getSourceId(),
                    settlement.getRewardProfileId(),
                    settlement.getRewardProfileCode(),
                    settlement.getStatus(),
                    settlement.getCreatedAt(),
                    settlement.getUpdatedAt(),
                    settlement.getLines().stream()
                            .map(Line::from)
                            .toList()
            );
        }
    }

    public record Line(
            Long lineId,
            Long rewardDefinitionId,
            String rewardDefinitionCode,
            RewardType rewardType,
            long amount,
            Long itemId,
            String itemCode,
            int sortOrder,
            RewardSettlementLineStatus status,
            String failureCode,
            Instant createdAt,
            Instant updatedAt
    ) {
        private static Line from(RewardSettlementLine line) {
            return new Line(
                    line.getId(),
                    line.getRewardDefinitionId(),
                    line.getRewardDefinitionCode(),
                    line.getRewardType(),
                    line.getAmount(),
                    line.getItemId(),
                    line.getItemCode(),
                    line.getSortOrder(),
                    line.getStatus(),
                    line.getFailureCode(),
                    line.getCreatedAt(),
                    line.getUpdatedAt()
            );
        }
    }
}
