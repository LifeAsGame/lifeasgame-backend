package online.lifeasgame.user.api.user.mapper;

import online.lifeasgame.user.api.user.request.UserRequest;
import online.lifeasgame.user.api.user.response.UserResponse;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.result.UserResult;

import java.util.List;

public final class UserWebMapper {

    private UserWebMapper() {}

    public static UserResponse.Created toCreated(UserResult.Created result) {
        return new UserResponse.Created(result.id());
    }

    public static UserResponse.UserInfo toUserInfo(UserResult.UserInfo result) {
        boolean playerExists = result.playerId() != null;
        return new UserResponse.UserInfo(
                new UserResponse.UserInfo.UserSummary(
                        result.userId(),
                        result.email(),
                        result.nickname(),
                        result.status()
                ),
                new UserResponse.UserInfo.PlayerHint(
                        playerExists,
                        result.playerId()
                ),
                new UserResponse.UserInfo.UiHint(
                        playerExists ? List.of() : List.of("LINK_START"),
                        new UserResponse.UserInfo.UiHint.Badges(0, 0)
                )
        );
    }

    public static UserResponse.Availability toAvailability(UserResult.Availability result) {
        return new UserResponse.Availability(result.isAvailable(), result.reason());
    }

    public static UserResponse.NicknameChanged toNicknameChanged(UserResult.NicknameChanged result) {
        return new UserResponse.NicknameChanged(
                result.userId(),
                result.oldNickname(),
                result.newNickname(),
                result.changedAt()
        );
    }

    public static UserCommand.ChangePassword toChangePasswordCommand(UserRequest.ChangePassword request) {
        return new UserCommand.ChangePassword(request.currentPassword(), request.newPassword());
    }

    public static UserResponse.PasswordChanged toPasswordChanged(UserResult.PasswordChanged result) {
        return new UserResponse.PasswordChanged(result.userId());
    }

    public static UserResponse.Deleted toDeleted(UserResult.Deleted result) {
        return new UserResponse.Deleted(result.userId(), result.status());
    }
}
