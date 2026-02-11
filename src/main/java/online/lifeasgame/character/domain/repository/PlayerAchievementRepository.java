package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.PlayerAchievement;

public interface PlayerAchievementRepository {
    PlayerAchievement save(PlayerAchievement playerAchievement);

    void deleteByPlayerIdAndAchievementId(Long playerId, Long achievementId);
}
