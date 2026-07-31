package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.application.event.QuestRewardReadyFact;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestCompletionRewardService {

    private final RewardSettlementCreateService settlementCreateService;
    private final RewardSettlementExpProcessService expProcessService;

    public void process(QuestRewardReadyFact fact) {
        RewardSettlement settlement = settlementCreateService.create(
                fact.playerId(),
                RewardSettlementSourceType.QUEST_COMPLETION,
                fact.acceptanceId(),
                fact.rewardProfileCode()
        );

        for (RewardSettlementLine line : settlement.getLines()) {
            switch (line.getRewardType()) {
                case EXP -> processExp(settlement.getId(), line.getId());
                case ITEM -> {
                    // ITEM settlement lines intentionally remain PENDING.
                }
            }
        }
    }

    private void processExp(Long settlementId, Long lineId) {
        try {
            expProcessService.process(settlementId, lineId);
        } catch (DomainException exception) {
            log.warn(
                    "Quest reward EXP failed: settlementId={}, lineId={}, failureCode={}",
                    settlementId,
                    lineId,
                    exception.getErrorCode().code()
            );
        }
    }
}
