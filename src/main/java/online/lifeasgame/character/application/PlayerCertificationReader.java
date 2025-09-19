package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerCertificationQuery;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerCertificationReader {

    private final PlayerCertificationQuery playerCertificationQuery;

    public List<PlayerCertificationView> getPlayerCertificationInfos(Long playerId) {
        return playerCertificationQuery.findPlayerCertificationInfos(playerId);
    }
}
