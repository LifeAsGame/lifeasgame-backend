package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardSettlementGoldProcessor {
    private final RewardSettlementGoldProcessAttempt attempt;
    private final RewardSettlementLineFailureRecorder failureRecorder;

    public void process(Long settlementId, Long lineId) {
        try {
            attempt.process(settlementId, lineId);
        } catch (DomainException exception) {
            failureRecorder.record(settlementId, lineId, exception.getErrorCode());
            throw exception;
        }
    }
}
