package online.lifeasgame.reward.infra;

import online.lifeasgame.reward.domain.RewardDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRewardDefinitionRepository extends JpaRepository<RewardDefinition, Long> {

    Optional<RewardDefinition> findByCode(String code);
}
