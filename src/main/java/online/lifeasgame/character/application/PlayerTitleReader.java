package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.application.view.PlayerTitleView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerTitleReader {

    private final PlayerTitleQuery playerTitleQuery;

    public List<PlayerTitleView> getPlayerTitleInfos(Long playerId) {
        return playerTitleQuery.findPlayerTitleInfos(playerId);
    }
}
