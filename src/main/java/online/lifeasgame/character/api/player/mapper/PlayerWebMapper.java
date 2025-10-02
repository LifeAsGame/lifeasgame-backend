package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.api.player.request.PlayerRequest;
import online.lifeasgame.character.api.player.response.PlayerResponse;

public class PlayerWebMapper {

    private PlayerWebMapper() {
    }

    public static PlayerCommand.Register toCommand(PlayerRequest.Register request) {
        return PlayerCommand.Register.of(request.name(), request.gender());
    }

    public static PlayerResponse.Created toCreated(PlayerResult.Created result) {
        return new PlayerResponse.Created(result.id());
    }

    public static PlayerResponse.Info toPlayerInfo(PlayerResult.PlayerInfo result) {
        return PlayerResponse.Info.of(
                result.name(),
                result.gender(),
                result.job(),
                result.level(),
                result.exp(),
                result.currentHealth(),
                result.healthCapacity(),
                result.currentMana(),
                result.manaCapacity(),
                result.str(),
                result.agi(),
                result.dex(),
                result.intel(),
                result.vit(),
                result.luc(),
                result.extraStats(),
                result.effects().stream()
                        .map((effect) -> PlayerResponse.Info.StatusEffects.of(effect.name(), effect.category().name()))
                        .toList()
        );
    }

    public static PlayerResponse.Updated toUpdatedTitle(PlayerResult.UpdatedTitle updatedTitle) {
        return PlayerResponse.Updated.of(
                updatedTitle.titleId()
        );
    }
}
