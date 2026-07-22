package online.lifeasgame.reward.infra;

import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaRewardSettlementRepository extends JpaRepository<RewardSettlement, Long> {

    @Override
    @EntityGraph(attributePaths = "lines")
    Optional<RewardSettlement> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "lines")
    @Query("select settlement from RewardSettlement settlement where settlement.id = :settlementId")
    Optional<RewardSettlement> findByIdForUpdate(@Param("settlementId") Long settlementId);

    @EntityGraph(attributePaths = "lines")
    Optional<RewardSettlement> findByPlayerIdAndSourceTypeAndSourceId(
            Long playerId,
            RewardSettlementSourceType sourceType,
            Long sourceId
    );
}
