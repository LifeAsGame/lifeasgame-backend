package online.lifeasgame.user.presentation.mapper;

import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.presentation.request.UserRequest;
import online.lifeasgame.user.presentation.response.UserResponse;

public final class UserWebMapper {

    public static UserCommand.Register toCommand(UserRequest.Register req) {
        return UserCommand.Register.of(req.email(), req.password(), req.nickname());
    }

    public static UserResponse.Created toCreated(UserResult.Created userResult) {
        return UserResponse.Created.of(userResult.id());
    }
}
