package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerTitleFacade {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerTitleService playerTitleService;


    public List<PlayerTitleResult.Info> getPlayerTitleInfos() {
        return playerTitleService.getPlayerTitleInfos(currentPlayerAccessor.currentPlayerIdOrThrow());
    }
}
