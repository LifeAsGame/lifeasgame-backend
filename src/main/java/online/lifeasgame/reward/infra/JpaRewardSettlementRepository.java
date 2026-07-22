package online.lifeasgame.reward.infra;

import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRewardSettlementRepository extends JpaRepository<RewardSettlement, Long> {

    @Override
    @EntityGraph(attributePaths = "lines")
    Optional<RewardSettlement> findById(Long id);

    @EntityGraph(attributePaths = "lines")
    Optional<RewardSettlement> findByPlayerIdAndSourceTypeAndSourceId(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    );
}
