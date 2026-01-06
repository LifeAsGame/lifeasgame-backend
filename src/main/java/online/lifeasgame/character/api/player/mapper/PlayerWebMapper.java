package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.api.player.request.PlayerRequest;
import online.lifeasgame.character.api.player.response.PlayerResponse;

public final class PlayerWebMapper {

    private PlayerWebMapper() {
    }

    public static PlayerCommand.Register toRegisterCommand(PlayerRequest.Register request) {
        return new PlayerCommand.Register(request.name(), request.gender());
    }

    public static PlayerResponse.Created toCreated(PlayerResult.Created result) {
        return new PlayerResponse.Created(result.id());
    }

    public static PlayerResponse.Info toPlayerInfo(PlayerResult.PlayerInfo result) {
        return new PlayerResponse.Info(
                result.playerId(),
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
                        .map(
                                effect ->
                                        new PlayerResponse.Info.StatusEffects(
                                                effect.name(),
                                                effect.category().name()
                                        )
                        )
                        .toList(),
                result.representativeTitleId()
        );
    }

    public static PlayerResponse.UpdatedTitle toUpdatedTitle(PlayerResult.UpdatedTitle result) {
        return new PlayerResponse.UpdatedTitle(result.titleId());
    }
}
