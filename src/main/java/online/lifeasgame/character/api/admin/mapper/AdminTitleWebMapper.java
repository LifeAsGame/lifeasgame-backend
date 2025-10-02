package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse;
import online.lifeasgame.character.application.command.TitleCommand;
import online.lifeasgame.character.application.result.TitleResult;

public class AdminTitleWebMapper {

    private AdminTitleWebMapper() {}

    public static TitleCommand.CreateTitle toCommand(AdminTitleRequest.CreateTitle request) {
        return TitleCommand.CreateTitle.of(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }

    public static AdminTitleResponse.TitleInfo toTitleInfo(TitleResult.TitleInfo result) {
        return AdminTitleResponse.TitleInfo.of(
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
