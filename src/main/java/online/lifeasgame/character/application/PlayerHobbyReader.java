package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerHobbyQuery;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerHobbyReader {

    private final PlayerHobbyQuery query;

    public List<PlayerHobbyView> getViewsByPlayerId(Long playerId) {
        return query.findViewsByPlayerId(playerId);
    }
}
