package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCommand.Register;
import online.lifeasgame.character.application.result.PlayerResult.Created;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerFacade {

    private final CurrentUserAccessor currentUserAccessor;
    private final PlayerService playerService;

    public Created linkStart(Register register) {
        Long userId = currentUserAccessor.currentUserIdOrThrow();
        return playerService.linkStart(userId, register);
    }
}
