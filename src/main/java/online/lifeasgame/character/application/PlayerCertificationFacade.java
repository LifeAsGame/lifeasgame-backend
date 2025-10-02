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


    public List<PlayerCertificationResult.Info> getPlayerCertificationInfos() {
        return playerCertificationService.getPlayerCertificationInfos(getPlayerId());
    }

    public PlayerCertificationResult.Changed changePlayerCertification(PlayerCertificationCommand.Change command) {
        return playerCertificationService.changePlayerCertification(getPlayerId(), command);
    }

    public PlayerCertificationResult.Created createPlayerCertification(
            PlayerCertificationCommand.Create command
    ) {
        return playerCertificationService.createPlayerCertification(getPlayerId(), command);
    }

    public void deletePlayerCertification(Long certificationId) {
        playerCertificationService.deletePlayerCertification(getPlayerId(), certificationId);
    }

    private Long getPlayerId() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }
}
