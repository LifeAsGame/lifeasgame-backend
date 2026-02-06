package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse;
import online.lifeasgame.character.application.command.TitleCommand;
import online.lifeasgame.character.application.result.TitleResult;

import java.util.List;

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
                result.titleId(),
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }

    public static AdminTitleResponse.Infos toInfos(List<TitleResult.Info> results) {
        return new AdminTitleResponse.Infos(
                results.stream()
                        .map(
                                result -> new AdminTitleResponse.Info(
                                        result.titleId(),
                                        result.code(),
                                        result.name(),
                                        result.category(),
                                        result.descMd()
                                )
                        )
                        .toList()
        );
    }

    public static AdminTitleResponse.Deleted toDelete(TitleResult.Deleted result) {
        return new AdminTitleResponse.Deleted(result.titleId());
    }
}
