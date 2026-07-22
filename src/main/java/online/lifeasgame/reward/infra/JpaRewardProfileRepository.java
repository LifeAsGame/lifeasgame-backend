package online.lifeasgame.reward.infra;

import online.lifeasgame.reward.domain.RewardProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRewardProfileRepository extends JpaRepository<RewardProfile, Long> {

    @EntityGraph(attributePaths = {"lines", "lines.rewardDefinition"})
    Optional<RewardProfile> findByCode(String code);
}
