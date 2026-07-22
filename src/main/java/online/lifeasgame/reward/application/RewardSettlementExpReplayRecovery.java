package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerGrowthApi;
import online.lifeasgame.character.application.internal.PlayerGrowthApi.PlayerGrowthGrantResult;
import online.lifeasgame.reward.application.result.RewardSettlementExpProcessResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RewardSettlementExpReplayRecovery {

    private final RewardSettlementReader settlementReader;
    private final PlayerGrowthApi playerGrowthApi;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<RewardSettlementExpProcessResult> findCompletedReplay(
            Long settlementId,
            Long lineId
    ) {
        RewardSettlement settlement = settlementReader.getByIdOrThrow(settlementId);
        RewardSettlementLine line = settlement.getLineByIdOrThrow(lineId);
        if (line.getStatus() != RewardSettlementLineStatus.SUCCEEDED) {
            return Optional.empty();
        }
        return playerGrowthApi.findRewardExpGrant(lineId)
                .filter(result -> matches(settlement, line, result))
                .map(result -> new RewardSettlementExpProcessResult(
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
                        true,
                        result.growthChangeId()
                ));
    }

    private boolean matches(
            RewardSettlement settlement,
            RewardSettlementLine line,
            PlayerGrowthGrantResult result
    ) {
        return settlement.getPlayerId().equals(result.playerId())
                && line.getId().equals(result.rewardLineId())
                && line.getAmount() == result.requestedExp();
    }
}
