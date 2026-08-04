package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.event.QuestRewardReadyFact;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestCompletionRewardService {

    private final RewardSettlementCreateService settlementCreateService;
    private final RewardSettlementReader settlementReader;
    private final RewardSettlementExpProcessService expProcessService;
    private final RewardSettlementItemProcessService itemProcessService;

    public void process(QuestRewardReadyFact fact) {
        RewardSettlement settlement = settlementCreateService.create(
                fact.playerId(),
                RewardSettlementSourceType.QUEST_COMPLETION,
                fact.acceptanceId(),
                fact.rewardProfileCode()
        );

        for (RewardSettlementLine line : settlement.getLines()) {
            switch (line.getRewardType()) {
                case EXP -> {
                    if (line.getStatus() == RewardSettlementLineStatus.FAILED) {
                        logFailed(settlement.getId(), line);
                    } else {
                        processExp(settlement.getId(), line.getId());
                    }
                }
                case ITEM -> {
                    if (line.getStatus() == RewardSettlementLineStatus.FAILED) {
                        logFailed(settlement.getId(), line);
                    } else {
                        processItem(settlement.getId(), line.getId());
                    }
                }
            }
        }
    }

    private void processItem(Long settlementId, Long lineId) {
        try {
            itemProcessService.process(settlementId, lineId);
        } catch (DomainException exception) {
            RewardSettlement fresh = settlementReader
                    .getByIdInNewTransactionOrThrow(settlementId);
            RewardSettlementLine freshLine = fresh.getLineByIdOrThrow(lineId);
            if (freshLine.getStatus() != RewardSettlementLineStatus.FAILED) {
                throw exception;
            }
            logFailed(settlementId, freshLine);
        }
    }

    private void processExp(Long settlementId, Long lineId) {
        try {
            expProcessService.process(settlementId, lineId);
        } catch (DomainException exception) {
            RewardSettlement fresh =
                    settlementReader.getByIdInNewTransactionOrThrow(
                            settlementId
                    );
            RewardSettlementLine freshLine =
                    fresh.getLineByIdOrThrow(lineId);
            if (freshLine.getStatus()
                    != RewardSettlementLineStatus.FAILED) {
                throw exception;
            }
            logFailed(settlementId, freshLine);
        }
    }

    private void logFailed(
            Long settlementId,
            RewardSettlementLine line
    ) {
        log.warn(
                "Quest reward {} failed: settlementId={}, lineId={}, failureCode={}",
                line.getRewardType(),
                settlementId,
                line.getId(),
                line.getFailureCode()
        );
    }
}
