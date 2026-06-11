package online.lifeasgame.character.application.query;

import online.lifeasgame.character.application.view.PlayerTitleView;

import java.util.List;

public interface PlayerTitleQuery {
    List<PlayerTitleView> findViewsByPlayerId(Long playerId);
}
