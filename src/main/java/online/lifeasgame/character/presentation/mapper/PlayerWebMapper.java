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

    public static PlayerResponse.PlayerInfo toPlayerInfo(PlayerResult.PlayerInfo playerInfo) {
        return PlayerResponse.PlayerInfo.of(
                playerInfo.name(),
                playerInfo.gender(),
                playerInfo.job(),
                playerInfo.level(),
                playerInfo.exp(),
                playerInfo.currentHealth(),
                playerInfo.healthCapacity(),
                playerInfo.currentMana(),
                playerInfo.manaCapacity(),
                playerInfo.str(),
                playerInfo.agi(),
                playerInfo.dex(),
                playerInfo.intel(),
                playerInfo.vit(),
                playerInfo.luc(),
                playerInfo.extraStats(),
                playerInfo.effects()
        );
    }
}
