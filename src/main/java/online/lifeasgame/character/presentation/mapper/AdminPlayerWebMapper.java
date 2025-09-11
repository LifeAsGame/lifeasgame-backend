package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.AdminPlayerCommand;
import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.presentation.request.AdminPlayerRequest;
import online.lifeasgame.character.presentation.response.AdminPlayerResponse;

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
}
