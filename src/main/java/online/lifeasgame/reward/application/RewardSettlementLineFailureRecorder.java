package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RewardSettlementLineFailureRecorder {

    private final RewardSettlementReader settlementReader;
    private final RewardSettlementWriter settlementWriter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long settlementId, Long lineId, ErrorCode errorCode) {
        settlementReader.findByIdForUpdate(settlementId)
                .filter(settlement -> settlement.markLineFailedIfPending(lineId, errorCode))
                .ifPresent(settlementWriter::saveAndFlush);
    }
}
