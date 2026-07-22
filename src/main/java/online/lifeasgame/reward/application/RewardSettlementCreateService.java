package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardSettlementCreateService {

    private final RewardSettlementReader settlementReader;
    private final RewardSettlementCreateAttempt createAttempt;

    public RewardSettlement create(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId,
            String rewardProfileCode
    ) {
        return settlementReader.findByIdentity(playerId, sourceType, sourceId)
                .orElseGet(() -> createNew(playerId, sourceType, sourceId, rewardProfileCode));
    }

    private RewardSettlement createNew(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId,
            String rewardProfileCode
    ) {
        try {
            return createAttempt.create(playerId, sourceType, sourceId, rewardProfileCode);
        } catch (DataIntegrityViolationException exception) {
            return settlementReader.findByIdentityInNewTransaction(playerId, sourceType, sourceId)
                    .orElseThrow(() -> exception);
        }
    }
}
