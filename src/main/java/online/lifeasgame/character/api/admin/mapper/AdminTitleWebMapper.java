package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse;
import online.lifeasgame.character.application.command.TitleCommand;
import online.lifeasgame.character.application.result.TitleResult;

public final class AdminTitleWebMapper {

    private AdminTitleWebMapper() {}

    public static TitleCommand.Create toCreateCommand(AdminTitleRequest.Create request) {
        return new TitleCommand.Create(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }

    public static AdminTitleResponse.Info toInfo(TitleResult.Info result) {
        return new AdminTitleResponse.Info(
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
