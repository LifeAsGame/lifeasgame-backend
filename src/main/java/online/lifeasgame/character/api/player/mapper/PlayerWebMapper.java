package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.GrowthResult;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.api.player.request.PlayerRequest;
import online.lifeasgame.character.api.player.response.PlayerResponse;

public final class PlayerWebMapper {

    private PlayerWebMapper() {
    }

    public static PlayerCommand.Register toRegisterCommand(PlayerRequest.Register request) {
        return new PlayerCommand.Register(request.name(), request.gender());
    }

    public static PlayerResponse.CreatedWithToken toCreatedWithToken(PlayerResult.CreatedWithToken result) {
        return new PlayerResponse.CreatedWithToken(result.id(), result.accessToken(), result.refreshToken());
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
                                                effect.code(),
                                                effect.category()
                                        )
                        )
                        .toList(),
                result.representativeTitleId()
        );
    }

    public static PlayerResponse.UpdatedTitle toUpdatedTitle(PlayerResult.UpdatedTitle result) {
        return new PlayerResponse.UpdatedTitle(result.titleId());
    }

    public static PlayerResponse.Growth toGrowth(GrowthResult.Overview result) {
        GrowthResult.Current current = result.current();
        return new PlayerResponse.Growth(
                new PlayerResponse.Growth.Current(
                        current.level(),
                        current.exp(),
                        current.str(),
                        current.agi(),
                        current.dex(),
                        current.intel(),
                        current.vit(),
                        current.luc(),
                        current.extraStats(),
                        current.representativeTitleId()
                ),
                result.recentExpChanges().stream()
                        .map(change -> new PlayerResponse.Growth.RecentExpChange(
                                change.changeId(),
                                change.requestedExp(),
                                change.appliedExp(),
                                change.leftoverExp(),
                                change.beforeLevel(),
                                change.afterLevel(),
                                change.beforeTotalExp(),
                                change.afterTotalExp(),
                                change.occurredAt(),
                                change.sourceType(),
                                change.sourceId()
                        ))
                        .toList()
        );
    }
}
