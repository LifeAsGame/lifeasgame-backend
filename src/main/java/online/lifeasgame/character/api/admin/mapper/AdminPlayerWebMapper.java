package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.application.command.AdminPlayerCommand;
import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;

public class AdminPlayerWebMapper {

    private AdminPlayerWebMapper() {
    }

    public static AdminPlayerResponse.ExpGranted toExpGranted(AdminPlayerResult.ExpGranted result) {
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

    public static AdminPlayerCommand.ChangeHp toCommand(Long playerId, AdminPlayerRequest.ChangeHp request) {
        return AdminPlayerCommand.ChangeHp.of(playerId, request.hpDelta());
    }

    public static AdminPlayerCommand.ChangeHpCapacity toCommand(Long playerId, AdminPlayerRequest.ChangeHpCapacity request) {
        return AdminPlayerCommand.ChangeHpCapacity.of(playerId, request.hpCapacityDelta());
    }

    public static AdminPlayerCommand.ChangeMp toCommand(Long playerId, AdminPlayerRequest.ChangeMp request) {
        return AdminPlayerCommand.ChangeMp.of(playerId, request.mpDelta());
    }

    public static AdminPlayerCommand.ChangeMpCapacity toCommand(Long playerId, AdminPlayerRequest.ChangeMpCapacity request) {
        return AdminPlayerCommand.ChangeMpCapacity.of(playerId, request.mpCapacityDelta());
    }

    public static AdminPlayerCommand.GrantCoreStats toCommand(Long playerId, AdminPlayerRequest.GrantCoreStats request) {
        return new AdminPlayerCommand.GrantCoreStats(
                playerId,
                request.nStr(),
                request.nAgi(),
                request.nDex(),
                request.nInt(),
                request.nVit(),
                request.nLuc()
        );
    }

    public static AdminPlayerCommand.GrantStatusEffects toCommand(Long playerId,
                                                                  AdminPlayerRequest.GrantStatusEffects request) {
        return AdminPlayerCommand.GrantStatusEffects.of(
                playerId,
                request.codes()
        );
    }

    public static AdminPlayerResponse.CurrentHp toCurrentHp(AdminPlayerResult.CurrentHp result) {
        return AdminPlayerResponse.CurrentHp.of(
                result.value()
        );
    }

    public static AdminPlayerResponse.HpCapacity toHpCapacity(AdminPlayerResult.HpCapacity result) {
        return AdminPlayerResponse.HpCapacity.of(
                result.cap()
        );
    }

    public static AdminPlayerResponse.CurrentMp toCurrentMp(AdminPlayerResult.CurrentMp result) {
        return AdminPlayerResponse.CurrentMp.of(
                result.value()
        );
    }

    public static AdminPlayerResponse.MpCapacity toMpCapacity(AdminPlayerResult.MpCapacity result) {
        return AdminPlayerResponse.MpCapacity.of(
                result.cap()
        );
    }

    public static AdminPlayerResponse.CoreStatsGranted toCoreStatsGranted(AdminPlayerResult.CoreStatsGranted result) {
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
            AdminPlayerResult.StatusEffectsGranted result
    ) {
        return AdminPlayerResponse.StatusEffectsGranted.of(
                result.playerId(),
                result.effects().stream()
                        .map(i -> new AdminPlayerResponse.StatusEffectsGranted.Item(i.code(), i.category()))
                        .toList()
        );
    }

    public static AdminPlayerResponse.UpdatedTitle toUpdatedTitle(AdminPlayerResult.UpdatedTitle updatedTitle) {
        return AdminPlayerResponse.UpdatedTitle.of(
                updatedTitle.titleId()
        );
    }
}
