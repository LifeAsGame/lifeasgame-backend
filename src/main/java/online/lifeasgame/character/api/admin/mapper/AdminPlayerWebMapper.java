package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;

public final class AdminPlayerWebMapper {

    private AdminPlayerWebMapper() {}

    public static AdminPlayerResponse.ExpGranted toExpGranted(PlayerResult.ExpGranted result) {
        return new AdminPlayerResponse.ExpGranted(
                result.playerId(),
                result.requestedExp(),
                result.appliedExp(),
                result.leftoverExp(),
                result.level(),
                result.totalExp(),
                result.expIntoLevel(),
                result.expToNext(),
                result.capForLevel(),
                result.progressRatio()
        );
    }

    public static PlayerCommand.ChangeHp toChangeHpCommand(Long playerId, AdminPlayerRequest.ChangeHp request) {
        return new PlayerCommand.ChangeHp(playerId, request.hpDelta());
    }

    public static AdminPlayerResponse.CurrentHp toCurrentHp(PlayerResult.CurrentHp result) {
        return new AdminPlayerResponse.CurrentHp(result.value());
    }

    public static PlayerCommand.ChangeHpCapacity toChangeHpCapacityCommand(Long playerId, AdminPlayerRequest.ChangeHpCapacity request) {
        return new PlayerCommand.ChangeHpCapacity(playerId, request.hpCapacityDelta());
    }

    public static AdminPlayerResponse.HpCapacity toHpCapacity(PlayerResult.HpCapacity result) {
        return new AdminPlayerResponse.HpCapacity(result.cap());
    }

    public static PlayerCommand.ChangeMp toChangeMpCommand(Long playerId, AdminPlayerRequest.ChangeMp request) {
        return new PlayerCommand.ChangeMp(playerId, request.mpDelta());
    }

    public static AdminPlayerResponse.CurrentMp toCurrentMp(PlayerResult.CurrentMp result) {
        return new AdminPlayerResponse.CurrentMp(result.value());
    }

    public static PlayerCommand.ChangeMpCapacity toChangeMpCapacityCommand(Long playerId, AdminPlayerRequest.ChangeMpCapacity request) {
        return new PlayerCommand.ChangeMpCapacity(playerId, request.mpCapacityDelta());
    }

    public static AdminPlayerResponse.MpCapacity toMpCapacity(PlayerResult.MpCapacity result) {
        return new AdminPlayerResponse.MpCapacity(result.cap());
    }

    public static PlayerCommand.GrantCoreStats toGrantCoreStatsCommand(Long playerId, AdminPlayerRequest.GrantCoreStats request) {
        return new PlayerCommand.GrantCoreStats(
                playerId,
                request.nStr(),
                request.nAgi(),
                request.nDex(),
                request.nInt(),
                request.nVit(),
                request.nLuc()
        );
    }

    public static AdminPlayerResponse.CoreStatsGranted toCoreStatsGranted(PlayerResult.CoreStatsGranted result) {
        return new AdminPlayerResponse.CoreStatsGranted(
                result.playerId(),
                result.str(),
                result.agi(),
                result.dex(),
                result.intel(),
                result.vit(),
                result.luc()
        );
    }

    public static PlayerCommand.GrantStatusEffects toGrantStatusEffectsCommand(
            Long playerId,
            AdminPlayerRequest.GrantStatusEffects request
    ) {
        return new PlayerCommand.GrantStatusEffects(playerId, request.codes());
    }

    public static AdminPlayerResponse.StatusEffectsGranted toStatusEffectsGranted(
            PlayerResult.StatusEffectsGranted result
    ) {
        return new AdminPlayerResponse.StatusEffectsGranted(
                result.playerId(),
                result.effects().stream()
                        .map(i -> new AdminPlayerResponse.StatusEffectsGranted.Item(i.code(), i.category()))
                        .toList()
        );
    }

    public static AdminPlayerResponse.UpdatedTitle toUpdatedTitle(PlayerResult.UpdatedTitle updatedTitle) {
        return new AdminPlayerResponse.UpdatedTitle(updatedTitle.titleId());
    }

    public static AdminPlayerResponse.PlayerInfo toPlayerInfo(PlayerResult.PlayerInfo result) {
        return new AdminPlayerResponse.PlayerInfo(
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
                result.effects().stream()
                        .map(
                                effect -> new AdminPlayerResponse.PlayerInfo.StatusEffect(
                                        effect.code(),
                                        effect.category()
                                )
                        )
                        .toList(),
                result.representativeTitleId()
        );
    }

    public static AdminPlayerResponse.Players toPlayers(PlayerResult.Players results) {
        return new AdminPlayerResponse.Players(
                results.players().stream()
                .map(
                        result -> new AdminPlayerResponse.Players.Item(
                                    result.playerId(),
                                    result.userId(),
                                    result.name()
                        )
                )
                .toList()
        );
    }
}
