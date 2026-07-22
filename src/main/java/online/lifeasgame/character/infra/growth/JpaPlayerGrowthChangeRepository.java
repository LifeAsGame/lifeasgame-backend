package online.lifeasgame.character.infra.growth;

import online.lifeasgame.character.domain.growth.PlayerGrowthChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPlayerGrowthChangeRepository extends JpaRepository<PlayerGrowthChange, Long> {

    Optional<PlayerGrowthChange> findByRewardLineId(Long rewardLineId);
}
