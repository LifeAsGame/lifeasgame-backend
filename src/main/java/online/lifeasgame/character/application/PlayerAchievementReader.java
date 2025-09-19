package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerAchievementQuery;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerAchievementReader {

    private final PlayerAchievementQuery playerAchievementQuery;

    public List<PlayerAchievementView> getPlayerAchievementInfos(Long playerId) {
        return playerAchievementQuery.findPlayerAchievementInfos(playerId);
    }
}
