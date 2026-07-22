package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.application.result.RewardSettlementLineRetryPreparationResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RewardSettlementLineRetryPreparationService {

    private final RewardSettlementReader settlementReader;
    private final RewardSettlementWriter settlementWriter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RewardSettlementLineRetryPreparationResult prepare(Long settlementId, Long lineId) {
        RewardSettlement settlement = settlementReader.getByIdForUpdateOrThrow(settlementId);
        boolean changed = settlement.prepareLineRetry(lineId);
        if (changed) {
            settlementWriter.saveAndFlush(settlement);
        }
        RewardSettlementLine line = settlement.getLineByIdOrThrow(lineId);
        return new RewardSettlementLineRetryPreparationResult(
                settlement.getId(),
                line.getId(),
                line.getStatus(),
                settlement.getStatus(),
                changed
        );
    }
}
