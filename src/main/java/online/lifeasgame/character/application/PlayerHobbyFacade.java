package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PlayerHobbyFacade {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final PlayerHobbyService playerHobbyService;


    public List<PlayerHobbyResult.Info> getPlayerHobbyInfos() {
        return playerHobbyService.getPlayerHobbyInfos(getPlayerId());
    }

    public PlayerHobbyResult.Created createPlayerHobby(
            PlayerHobbyCommand.Create command
    ) {
        return playerHobbyService.createPlayerHobby(getPlayerId(), command);
    }

    public PlayerHobbyResult.Changed changePlayerHobby(PlayerHobbyCommand.Change command) {
        return playerHobbyService.changePlayerHobby(getPlayerId(), command);
    }

    public void deletePlayerHobby(Long hobbyId) {
        playerHobbyService.deletePlayerHobby(getPlayerId(), hobbyId);
    }

    private Long getPlayerId() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }
}
