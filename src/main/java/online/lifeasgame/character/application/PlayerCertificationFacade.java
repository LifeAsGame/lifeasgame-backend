package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerCertificationFacade {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerCertificationService playerCertificationService;


    public List<PlayerCertificationResult.PlayerCertificationInfo> getPlayerCertificationInfos() {
        return playerCertificationService.getPlayerCertificationInfos(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public PlayerCertificationResult.ChangedPlayerCertification changePlayerCertification(PlayerCertificationCommand.ChangePlayerCertification command) {
        return playerCertificationService.changePlayerCertification(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }
}
