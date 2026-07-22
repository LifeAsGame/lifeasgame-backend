package online.lifeasgame.reward.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.repository.RewardDefinitionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardDefinitionRepositoryAdapter implements RewardDefinitionRepository {

    private final JpaRewardDefinitionRepository jpaRepository;

    @Override
    public RewardDefinition save(RewardDefinition rewardDefinition) {
        return jpaRepository.save(rewardDefinition);
    }

    @Override
    public Optional<RewardDefinition> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }
}
