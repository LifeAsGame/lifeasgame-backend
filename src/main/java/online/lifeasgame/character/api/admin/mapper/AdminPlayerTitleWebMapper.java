package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;
import online.lifeasgame.character.application.result.PlayerTitleResult;

public class AdminPlayerTitleWebMapper {

    private AdminPlayerTitleWebMapper() {
    }

    public static AdminPlayerTitleResponse.Granted toGrantedTitle(
            PlayerTitleResult.Granted result
    ) {
        return AdminPlayerTitleResponse.Granted.of(
                result.playerId(),
                result.titleId(),
                result.code(),
                result.name(),
                result.category(),
                result.acquiredAt()
        );
    }
}
