package online.lifeasgame.reward.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.repository.RewardSettlementRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardSettlementRepositoryAdapter implements RewardSettlementRepository {

    private final JpaRewardSettlementRepository jpaRepository;

    @Override
    public RewardSettlement saveAndFlush(RewardSettlement settlement) {
        return jpaRepository.saveAndFlush(settlement);
    }

    @Override
    public Optional<RewardSettlement> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<RewardSettlement> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    public Optional<RewardSettlement> findByIdentity(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    ) {
        return jpaRepository.findByPlayerIdAndSourceTypeAndSourceId(playerId, sourceType, sourceId);
    }
}
