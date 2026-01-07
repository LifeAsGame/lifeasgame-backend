package online.lifeasgame.user.api.user.mapper;

import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.api.user.request.UserRequest;
import online.lifeasgame.user.api.user.response.UserResponse;

public final class UserWebMapper {

    private UserWebMapper() {}

    public static UserCommand.Register toRegisterCommand(UserRequest.Register request) {
        return new UserCommand.Register(request.email(), request.password(), request.nickname());
    }

    public static UserResponse.Created toCreated(UserResult.Created result) {
        return new UserResponse.Created(result.id());
    }

    public static UserResponse.UserInfo toUserInfo(UserResult.UserInfo result) {
        return new UserResponse.UserInfo(
                null,
                null,
                null
        );
    }

    public static UserResponse.Availability toAvailability(UserResult.Availability result) {
        return new UserResponse.Availability(result.isAvailable(), result.reason());
    }
}
