package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.AdminPlayerCommand;
import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.presentation.request.AdminPlayerRequest;
import online.lifeasgame.character.presentation.response.AdminPlayerResponse;

public class AdminPlayerWebMapper {

    private AdminPlayerWebMapper() {
    }

    public static AdminPlayerResponse.ExpGranted toExpGranted(AdminPlayerResult.ExpGranted expGranted) {
        return AdminPlayerResponse.ExpGranted.of(
                expGranted.playerId(),
                expGranted.requestedExp(),
                expGranted.appliedExp(),
                expGranted.leftoverExp(),
                expGranted.level(),
                expGranted.totalExp(),
                expGranted.expIntoLevel(),
                expGranted.expToNext(),
                expGranted.capForLevel(),
                expGranted.progressRatio()
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

    public static AdminPlayerResponse.CoreStatsGranted toCoreStatsGranted(AdminPlayerResult.CoreStatsGranted coreStatsGranted) {
        return AdminPlayerResponse.CoreStatsGranted.of(
                coreStatsGranted.playerId(),
                coreStatsGranted.str(),
                coreStatsGranted.agi(),
                coreStatsGranted.dex(),
                coreStatsGranted.intel(),
                coreStatsGranted.vit(),
                coreStatsGranted.luc()
        );
    }
}
