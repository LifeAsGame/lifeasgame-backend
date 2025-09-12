package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.AdminTitleCommand;
import online.lifeasgame.character.application.result.AdminTitleResult;
import online.lifeasgame.character.presentation.request.AdminTitleRequest;
import online.lifeasgame.character.presentation.response.AdminTitleResponse;

public class AdminTitleWebMapper {

    private AdminTitleWebMapper() {}

    public static AdminTitleCommand.CreateTitle toCommand(AdminTitleRequest.CreateTitle request) {
        return AdminTitleCommand.CreateTitle.of(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }

    public static AdminTitleResponse.TitleInfo toTitleInfo(AdminTitleResult.TitleInfo result) {
        return AdminTitleResponse.TitleInfo.of(
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
