package online.lifeasgame.character.application.query;

import java.util.List;
import online.lifeasgame.character.application.view.PlayerCertificationView;

public interface PlayerCertificationQuery {
    List<PlayerCertificationView> findPlayerCertificationInfos(Long playerId);
}
