package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerAchievementFacade {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerAchievementService playerAchievementService;


    public List<PlayerAchievementResult.PlayerAchievementInfo> getPlayerAchievementInfos() {
        return playerAchievementService.getPlayerAchievementInfos(currentPlayerAccessor.currentPlayerIdOrThrow());
    }
}
