package online.lifeasgame.character.application.query;

import online.lifeasgame.character.application.view.PlayerAchievementView;

import java.util.List;
import java.util.Optional;

public interface PlayerAchievementQuery {
    List<PlayerAchievementView> findViewsByPlayerId(Long playerId);

    Optional<PlayerAchievementView> findViewByPlayerIdAndAchievementId(Long playerId, Long achievementId);
}
