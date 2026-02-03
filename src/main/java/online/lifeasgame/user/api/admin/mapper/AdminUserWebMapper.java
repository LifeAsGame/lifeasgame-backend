package online.lifeasgame.user.api.admin.mapper;

import online.lifeasgame.user.api.admin.response.AdminUserResponse;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.result.UserResult;

import java.util.List;

public final class AdminUserWebMapper {

    private AdminUserWebMapper() {}


    public static UserCommand.Search toSearchCommand(String email, String nickname, String status, int page, int size) {
        return new UserCommand.Search(email, nickname, status, page, size);
    }

    public static AdminUserResponse.UserList toUserList(UserResult.UserList result) {
        List<AdminUserResponse.UserList.UserSummary> users = result.users().stream()
                .map(AdminUserWebMapper::toUserSummary)
                .toList();

        AdminUserResponse.UserList.PageInfo page = toPageInfo(result.page());

        return new AdminUserResponse.UserList(users, page);
    }

    public static AdminUserResponse.UserInfo toUserInfo(UserResult.UserInfo result) {
        return new AdminUserResponse.UserInfo(
                null,
                null,
                null
        );
    }


    private static AdminUserResponse.UserList.UserSummary toUserSummary(UserResult.UserList.UserSummary s) {
        return new AdminUserResponse.UserList.UserSummary(
                s.id(),
                s.email(),
                s.nickname(),
                s.status(),
                s.createdAt()
        );
    }

    private static AdminUserResponse.UserList.PageInfo toPageInfo(UserResult.UserList.PageInfo p) {
        return new AdminUserResponse.UserList.PageInfo(
                p.page(),
                p.size(),
                p.totalElements()
        );
    }
}
