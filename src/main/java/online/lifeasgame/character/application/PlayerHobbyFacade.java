package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerHobbyFacade {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerHobbyService playerHobbyService;


    public List<PlayerHobbyResult.PlayerHobbyInfo> getPlayerHobbyInfos() {
        return playerHobbyService.getPlayerHobbyInfos(getPlayerId());
    }

    public PlayerHobbyResult.CreatedPlayerHobby createPlayerHobby(
            PlayerHobbyCommand.CreatePlayerHobby command
    ) {
        return playerHobbyService.createPlayerHobby(getPlayerId(), command);
    }

    public PlayerHobbyResult.ChangedPlayerHobby changePlayerHobby(PlayerHobbyCommand.ChangePlayerHobby command) {
        return playerHobbyService.changePlayerHobby(getPlayerId(), command);
    }

    public void deletePlayerHobby(Long hobbyId) {
        playerHobbyService.deletePlayerHobby(getPlayerId(), hobbyId);
    }

    private Long getPlayerId() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }
}
