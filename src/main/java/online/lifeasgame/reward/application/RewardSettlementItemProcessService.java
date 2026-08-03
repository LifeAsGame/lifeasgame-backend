package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.result.RewardSettlementItemProcessResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardSettlementItemProcessService {

    private final RewardSettlementItemProcessAttempt processAttempt;
    private final RewardSettlementLineFailureRecorder failureRecorder;

    public RewardSettlementItemProcessResult process(
            Long settlementId,
            Long lineId
    ) {
        try {
            return processAttempt.process(settlementId, lineId);
        } catch (DomainException exception) {
            failureRecorder.record(
                    settlementId,
                    lineId,
                    exception.getErrorCode()
            );
            throw exception;
        }
    }
}
