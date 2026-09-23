package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.application.internal.RewardGoldCreditApi;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.error.RewardError;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RewardSettlementGoldProcessAttempt {
    private final RewardSettlementReader settlementReader;
    private final RewardSettlementWriter settlementWriter;
    private final RewardGoldCreditApi goldCreditApi;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long settlementId, Long lineId) {
        RewardSettlement settlement = settlementReader.getByIdForUpdateOrThrow(settlementId);
        RewardSettlementLine line = settlement.getLineByIdOrThrow(lineId);
        boolean required = line.isGoldProcessingRequired();
        if (settlement.getAccountId() == null) {
            throw new DomainException(RewardError.REWARD_GOLD_PAYLOAD_INVALID);
        }
        var expected = new RewardGoldCreditApi.Receipt(lineId, settlement.getPlayerId(),
                settlement.getAccountId(), line.getAmount());
        var receipt = required
                ? goldCreditApi.credit(lineId, settlement.getPlayerId(), settlement.getAccountId(), line.getAmount())
                : goldCreditApi.find(lineId).orElseThrow(() ->
                        new DomainException(RewardError.REWARD_GOLD_RECEIPT_INCONSISTENT));
        if (!expected.equals(receipt)) {
            throw new DomainException(RewardError.REWARD_GOLD_RECEIPT_INCONSISTENT);
        }
        if (required) {
            settlement.markLineSucceeded(line.getSortOrder());
            settlementWriter.saveAndFlush(settlement);
        }
    }
}
