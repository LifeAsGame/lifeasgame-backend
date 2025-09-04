package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.presentation.request.PlayerRequest;
import online.lifeasgame.character.presentation.response.PlayerResponse;

public class PlayerWebMapper {
    public static PlayerCommand.Register toCommand(PlayerRequest.Register register) {
        return PlayerCommand.Register.of(register.name(), register.gender());
    }

    public static PlayerResponse.Created toCreated(PlayerResult.Created playerResult) {
        return new PlayerResponse.Created(playerResult.id());
    }
}
