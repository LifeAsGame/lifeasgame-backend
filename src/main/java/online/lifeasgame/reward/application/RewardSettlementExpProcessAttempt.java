package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerGrowthApi;
import online.lifeasgame.character.application.internal.PlayerGrowthApi.PlayerGrowthGrantResult;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.result.RewardSettlementExpProcessResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.error.RewardError;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RewardSettlementExpProcessAttempt {

    private final RewardSettlementReader settlementReader;
    private final RewardSettlementWriter settlementWriter;
    private final PlayerGrowthApi playerGrowthApi;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RewardSettlementExpProcessResult process(Long settlementId, Long lineId) {
        RewardSettlement settlement = settlementReader.getByIdForUpdateOrThrow(settlementId);
        RewardSettlementLine line = settlement.getLineByIdOrThrow(lineId);

        if (!line.isExpProcessingRequired()) {
            PlayerGrowthGrantResult replay = playerGrowthApi.findRewardExpGrant(lineId)
                    .orElseThrow(() -> new DomainException(
                            RewardError.REWARD_SETTLEMENT_EXP_GROWTH_INCONSISTENT
                    ));
            assertMatches(settlement, line, replay);
            return toResult(settlement, line, replay);
        }

        PlayerGrowthGrantResult grantResult = playerGrowthApi.grantRewardExp(
                settlement.getPlayerId(),
                line.getId(),
                line.getAmount()
        );
        assertMatches(settlement, line, grantResult);
        if (grantResult.replayed()) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_EXP_GROWTH_INCONSISTENT);
        }

        settlement.markExpLineSucceeded(lineId);
        settlementWriter.saveAndFlush(settlement);

        return toResult(settlement, line, grantResult);
    }

    private void assertMatches(
            RewardSettlement settlement,
            RewardSettlementLine line,
            PlayerGrowthGrantResult result
    ) {
        if (!settlement.getPlayerId().equals(result.playerId())
                || !line.getId().equals(result.rewardLineId())
                || line.getAmount() != result.requestedExp()) {
            throw new DomainException(RewardError.REWARD_SETTLEMENT_EXP_GROWTH_INCONSISTENT);
        }
    }

    private RewardSettlementExpProcessResult toResult(
            RewardSettlement settlement,
            RewardSettlementLine line,
            PlayerGrowthGrantResult result
    ) {
        return new RewardSettlementExpProcessResult(
                settlement.getId(),
                line.getId(),
                settlement.getPlayerId(),
                line.getStatus(),
                settlement.getStatus(),
                result.requestedExp(),
                result.appliedExp(),
                result.leftoverExp(),
                result.beforeLevel(),
                result.afterLevel(),
                result.beforeTotalExp(),
                result.afterTotalExp(),
                result.replayed(),
                result.growthChangeId()
        );
    }
}
