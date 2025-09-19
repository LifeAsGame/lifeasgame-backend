package online.lifeasgame.character.application.query;

import java.util.List;
import online.lifeasgame.character.application.view.PlayerHobbyView;

public interface PlayerHobbyQuery {
    List<PlayerHobbyView> findPlayerHobbyInfos(Long playerId);
}
