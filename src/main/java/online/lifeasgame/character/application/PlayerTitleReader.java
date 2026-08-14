package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.application.view.PlayerTitleView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerTitleReader {

    private final PlayerTitleQuery query;

    public List<PlayerTitleView> getViewsByPlayerId(Long playerId) {
        return query.findViewsByPlayerId(playerId);
    }
}
