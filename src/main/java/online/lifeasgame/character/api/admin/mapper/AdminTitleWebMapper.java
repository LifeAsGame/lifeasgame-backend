package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse;
import online.lifeasgame.character.application.command.TitleCommand;
import online.lifeasgame.character.application.result.TitleResult;

public class AdminTitleWebMapper {

    private AdminTitleWebMapper() {}

    public static TitleCommand.Create toCommand(AdminTitleRequest.Create request) {
        return TitleCommand.Create.of(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }

    public static AdminTitleResponse.Info toTitleInfo(TitleResult.Info result) {
        return AdminTitleResponse.Info.of(
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
