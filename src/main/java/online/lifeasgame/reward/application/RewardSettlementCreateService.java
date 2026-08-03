package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.error.RewardError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
        RewardSettlement settlement = settlementReader
                .findByIdentity(playerId, sourceType, sourceId)
                .orElseGet(() -> createNew(playerId, sourceType, sourceId, rewardProfileCode));
        if (!Objects.equals(
                settlement.getRewardProfileCode(),
                rewardProfileCode
        )) {
            throw new DomainException(
                    RewardError.REWARD_SETTLEMENT_SOURCE_PROFILE_CONFLICT
            );
        }
        return settlement;
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
