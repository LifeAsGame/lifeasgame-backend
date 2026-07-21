package online.lifeasgame.reward.domain.repository;

import online.lifeasgame.reward.domain.RewardProfile;

import java.util.Optional;

public interface RewardProfileRepository {

    RewardProfile save(RewardProfile rewardProfile);

    Optional<RewardProfile> findByCode(String code);
}
