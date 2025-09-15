package online.lifeasgame.character.application.query;

import java.util.List;
import online.lifeasgame.character.application.view.PlayerAchievementView;

public interface PlayerAchievementQuery {
    List<PlayerAchievementView> findPlayerAchievementInfos(Long playerId);
}
