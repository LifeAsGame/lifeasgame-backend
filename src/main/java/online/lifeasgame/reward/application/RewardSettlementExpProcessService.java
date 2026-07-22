package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.result.RewardSettlementExpProcessResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardSettlementExpProcessService {

    private final RewardSettlementExpProcessAttempt processAttempt;
    private final RewardSettlementLineFailureRecorder failureRecorder;
    private final RewardSettlementExpReplayRecovery replayRecovery;

    public RewardSettlementExpProcessResult process(Long settlementId, Long lineId) {
        try {
            return processAttempt.process(settlementId, lineId);
        } catch (DomainException exception) {
            failureRecorder.record(settlementId, lineId, exception.getErrorCode());
            throw exception;
        } catch (DataIntegrityViolationException exception) {
            return replayRecovery.findCompletedReplay(settlementId, lineId)
                    .orElseThrow(() -> exception);
        }
    }
}
