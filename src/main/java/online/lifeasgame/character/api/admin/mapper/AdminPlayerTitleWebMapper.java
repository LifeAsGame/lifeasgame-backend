package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;
import online.lifeasgame.character.application.result.PlayerTitleResult;

public final class AdminPlayerTitleWebMapper {

    private AdminPlayerTitleWebMapper() {}

    public static AdminPlayerTitleResponse.Granted toGrantedTitle(PlayerTitleResult.Created result) {
        return new AdminPlayerTitleResponse.Granted(
                result.playerId(),
                result.titleId(),
                result.code(),
                result.name(),
                result.category(),
                result.acquiredAt()
        );
    }

    public static AdminPlayerTitleResponse.Revoked toRevoked(PlayerTitleResult.Revoked result) {
        return new AdminPlayerTitleResponse.Revoked(
                result.playerId(),
                result.titleId()
        );
    }
}
