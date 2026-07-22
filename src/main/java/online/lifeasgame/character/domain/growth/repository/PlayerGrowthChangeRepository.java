package online.lifeasgame.character.domain.growth.repository;

import online.lifeasgame.character.domain.growth.PlayerGrowthChange;

import java.util.Optional;

public interface PlayerGrowthChangeRepository {

    PlayerGrowthChange saveAndFlush(PlayerGrowthChange change);

    Optional<PlayerGrowthChange> findByRewardLineId(Long rewardLineId);
}
