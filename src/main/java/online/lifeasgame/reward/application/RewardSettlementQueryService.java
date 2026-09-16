package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.reward.application.result.RewardSettlementResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardSettlementQueryService {

    private final RewardSettlementReader settlementReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public RewardSettlementResult.Detail getQuestCompletionSettlement(
            Long questAcceptanceId
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        RewardSettlement settlement = settlementReader.getByIdentityOrThrow(
                playerId,
                RewardSettlementSourceType.QUEST_COMPLETION,
                questAcceptanceId
        );
        return RewardSettlementResult.Detail.from(settlement);
    }
}
