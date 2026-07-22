package online.lifeasgame.reward.domain.repository;

import online.lifeasgame.reward.domain.RewardDefinition;

import java.util.Optional;

public interface RewardDefinitionRepository {

    RewardDefinition save(RewardDefinition rewardDefinition);

    Optional<RewardDefinition> findByCode(String code);
}
