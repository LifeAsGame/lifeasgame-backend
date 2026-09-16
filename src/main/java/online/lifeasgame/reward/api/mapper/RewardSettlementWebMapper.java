package online.lifeasgame.reward.api.mapper;

import online.lifeasgame.reward.api.response.RewardSettlementResponse;
import online.lifeasgame.reward.application.result.RewardSettlementResult;

public final class RewardSettlementWebMapper {

    private RewardSettlementWebMapper() {
    }

    public static RewardSettlementResponse.Detail toDetail(
            RewardSettlementResult.Detail result
    ) {
        return new RewardSettlementResponse.Detail(
                result.settlementId(),
                result.sourceType().name(),
                result.sourceId(),
                result.rewardProfileId(),
                result.rewardProfileCode(),
                result.status().name(),
                result.createdAt(),
                result.updatedAt(),
                result.lines().stream()
                        .map(RewardSettlementWebMapper::toLine)
                        .toList()
        );
    }

    private static RewardSettlementResponse.Line toLine(
            RewardSettlementResult.Line line
    ) {
        return new RewardSettlementResponse.Line(
                line.lineId(),
                line.rewardDefinitionId(),
                line.rewardDefinitionCode(),
                line.rewardType().name(),
                line.amount(),
                line.itemId(),
                line.itemCode(),
                line.sortOrder(),
                line.status().name(),
                line.failureCode(),
                line.createdAt(),
                line.updatedAt()
        );
    }
}
