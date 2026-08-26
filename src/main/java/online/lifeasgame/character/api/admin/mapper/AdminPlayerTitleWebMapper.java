package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;
import online.lifeasgame.character.application.result.PlayerTitleResult;

import java.util.List;

public final class AdminPlayerTitleWebMapper {

    private AdminPlayerTitleWebMapper() {}

    public static AdminPlayerTitleResponse.Infos toInfos(
            Long playerId,
            List<PlayerTitleResult.Info> results
    ) {
        return new AdminPlayerTitleResponse.Infos(
                playerId,
                results.stream()
                        .map(result -> new AdminPlayerTitleResponse.Info(
                                result.titleId(),
                                result.code(),
                                result.name(),
                                result.category(),
                                result.acquiredAt()
                        ))
                        .toList()
        );
    }

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

    public static PlayerTitleResult.Update toUpdateCommand(AdminTitleRequest.Update request) {
        return new PlayerTitleResult.Update(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }
}
