package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerCertificationQuery;
import online.lifeasgame.character.application.view.PlayerCertificationView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerCertificationReader {

    private final PlayerCertificationQuery query;

    public List<PlayerCertificationView> getViewByPlayerId(Long playerId) {
        return query.findViewsByPlayerId(playerId);
    }
}
