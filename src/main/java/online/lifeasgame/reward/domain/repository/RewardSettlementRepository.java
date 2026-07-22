package online.lifeasgame.reward.domain.repository;

import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;

import java.util.Optional;

public interface RewardSettlementRepository {

    RewardSettlement saveAndFlush(RewardSettlement settlement);

    Optional<RewardSettlement> findById(Long id);

    Optional<RewardSettlement> findByIdentity(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    );
}
