package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.character.application.command.HobbyCommand;
import online.lifeasgame.character.application.result.HobbyResult;

import java.util.List;

public final class AdminHobbyWebMapper {

    private AdminHobbyWebMapper() {}

    public static HobbyCommand.Create toCreateCommand(AdminHobbyRequest.Create request) {
        return new HobbyCommand.Create(
                request.name(),
                request.category()
        );
    }

    public static AdminHobbyResponse.Info toInfo(HobbyResult.Info result) {
        return new AdminHobbyResponse.Info(
                result.hobbyId(),
                result.name(),
                result.category()
        );
    }

    public static AdminHobbyResponse.Infos toInfos(List<HobbyResult.Info> results) {
        return new AdminHobbyResponse.Infos(
                results.stream()
                        .map(
                                result -> new AdminHobbyResponse.Info(
                                        result.hobbyId(),
                                        result.name(),
                                        result.category()
                                )
                        )
                        .toList()
        );
    }

    public static HobbyCommand.Update toUpdateCommand(AdminHobbyRequest.Update request) {
        return new HobbyCommand.Update(
                request.name(),
                request.category()
        );
    }

    public static AdminHobbyResponse.Deleted toDeleted(HobbyResult.Deleted result) {
        return new AdminHobbyResponse.Deleted(result.hobbyId());
    }
}
