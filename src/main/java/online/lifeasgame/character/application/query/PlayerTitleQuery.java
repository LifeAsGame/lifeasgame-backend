package online.lifeasgame.character.application.query;

import java.util.List;
import online.lifeasgame.character.application.view.PlayerTitleView;

public interface PlayerTitleQuery {
    List<PlayerTitleView> findPlayerTitleInfos(Long playerId);
}
