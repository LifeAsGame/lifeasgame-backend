package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.application.result.AdminPlayerTitleResult;
import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;

public class AdminPlayerTitleWebMapper {

    private AdminPlayerTitleWebMapper() {
    }

    public static AdminPlayerTitleResponse.GrantedTitle toGrantedTitle(
            AdminPlayerTitleResult.GrantedTitle result
    ) {
        return AdminPlayerTitleResponse.GrantedTitle.of(
                result.playerId(),
                result.titleId(),
                result.code(),
                result.name(),
                result.category(),
                result.acquiredAt()
        );
    }
}
