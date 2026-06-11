package online.lifeasgame.character.application.query;

import online.lifeasgame.character.application.view.PlayerAchievementView;

import java.util.List;

public interface PlayerAchievementQuery {
    List<PlayerAchievementView> findViewsByPlayerId(Long playerId);
}
