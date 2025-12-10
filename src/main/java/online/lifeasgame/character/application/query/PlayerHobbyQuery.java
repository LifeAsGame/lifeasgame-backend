package online.lifeasgame.character.application.query;

import online.lifeasgame.character.application.view.PlayerHobbyView;

import java.util.List;

public interface PlayerHobbyQuery {
    List<PlayerHobbyView> findViewsByPlayerId(Long playerId);
}
