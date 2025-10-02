package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;

public class AdminPlayerWebMapper {

    private AdminPlayerWebMapper() {
    }

    public static AdminPlayerResponse.ExpGranted toExpGranted(PlayerResult.ExpGranted result) {
        return AdminPlayerResponse.ExpGranted.of(
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

    public static PlayerCommand.ChangeHp toCommand(Long playerId, AdminPlayerRequest.ChangeHp request) {
        return PlayerCommand.ChangeHp.of(playerId, request.hpDelta());
    }

    public static PlayerCommand.ChangeHpCapacity toCommand(Long playerId, AdminPlayerRequest.ChangeHpCapacity request) {
        return PlayerCommand.ChangeHpCapacity.of(playerId, request.hpCapacityDelta());
    }

    public static PlayerCommand.ChangeMp toCommand(Long playerId, AdminPlayerRequest.ChangeMp request) {
        return PlayerCommand.ChangeMp.of(playerId, request.mpDelta());
    }

    public static PlayerCommand.ChangeMpCapacity toCommand(Long playerId, AdminPlayerRequest.ChangeMpCapacity request) {
        return PlayerCommand.ChangeMpCapacity.of(playerId, request.mpCapacityDelta());
    }

    public static PlayerCommand.GrantCoreStats toCommand(Long playerId, AdminPlayerRequest.GrantCoreStats request) {
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

    public static PlayerCommand.GrantStatusEffects toCommand(
            Long playerId, AdminPlayerRequest.GrantStatusEffects request
    ) {
        return PlayerCommand.GrantStatusEffects.of(
                playerId,
                request.codes()
        );
    }

    public static AdminPlayerResponse.CurrentHp toCurrentHp(PlayerResult.CurrentHp result) {
        return AdminPlayerResponse.CurrentHp.of(
                result.value()
        );
    }

    public static AdminPlayerResponse.HpCapacity toHpCapacity(PlayerResult.HpCapacity result) {
        return AdminPlayerResponse.HpCapacity.of(
                result.cap()
        );
    }

    public static AdminPlayerResponse.CurrentMp toCurrentMp(PlayerResult.CurrentMp result) {
        return AdminPlayerResponse.CurrentMp.of(
                result.value()
        );
    }

    public static AdminPlayerResponse.MpCapacity toMpCapacity(PlayerResult.MpCapacity result) {
        return AdminPlayerResponse.MpCapacity.of(
                result.cap()
        );
    }

    public static AdminPlayerResponse.CoreStatsGranted toCoreStatsGranted(PlayerResult.CoreStatsGranted result) {
        return AdminPlayerResponse.CoreStatsGranted.of(
                result.playerId(),
                result.str(),
                result.agi(),
                result.dex(),
                result.intel(),
                result.vit(),
                result.luc()
        );
    }

    public static AdminPlayerResponse.StatusEffectsGranted toStatusEffectsGranted(
            PlayerResult.StatusEffectsGranted result
    ) {
        return AdminPlayerResponse.StatusEffectsGranted.of(
                result.playerId(),
                result.effects().stream()
                        .map(i -> new AdminPlayerResponse.StatusEffectsGranted.Item(i.code(), i.category()))
                        .toList()
        );
    }

    public static AdminPlayerResponse.UpdatedTitle toUpdatedTitle(PlayerResult.UpdatedTitle updatedTitle) {
        return AdminPlayerResponse.UpdatedTitle.of(
                updatedTitle.titleId()
        );
    }
}
