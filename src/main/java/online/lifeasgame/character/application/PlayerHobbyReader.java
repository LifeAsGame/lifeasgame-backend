package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerHobbyQuery;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerHobbyReader {

    private final PlayerHobbyQuery playerHobbyQuery;

    public List<PlayerHobbyView> getPlayerHobbyInfos(Long playerId) {
        return playerHobbyQuery.findPlayerHobbyInfos(playerId);
    }
}
