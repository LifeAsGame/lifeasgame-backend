package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.result.AdminPlayerResult;
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
}
