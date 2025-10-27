package online.lifeasgame.social.api.admin.mapper;

import online.lifeasgame.social.api.admin.request.AdminFollowRequest;
import online.lifeasgame.social.api.admin.response.AdminFollowResponse;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.FollowResult;

import java.util.List;

public final class AdminFollowWebMapper {
    private AdminFollowWebMapper() {
    }

    // Request -> Command
    public static FollowCommand.Create toCommand(AdminFollowRequest.Create r) {
        return FollowCommand.Create.of(r.targetPlayerId());
    }

    public static FollowCommand.Unfollow toUnfollow(AdminFollowRequest.Empty r) {
        return FollowCommand.Unfollow.of();
    }

    public static FollowCommand.Mute toMute(AdminFollowRequest.Empty r) {
        return FollowCommand.Mute.of();
    }

    public static FollowCommand.Unmute toUnmute(AdminFollowRequest.Empty r) {
        return FollowCommand.Unmute.of();
    }

    public static FollowCommand.Block toBlock(AdminFollowRequest.Empty r) {
        return FollowCommand.Block.of();
    }

    public static FollowCommand.Unblock toUnblock(AdminFollowRequest.Empty r) {
        return FollowCommand.Unblock.of();
    }

    // Result -> Response
    public static AdminFollowResponse.Summary toSummary(FollowResult.Summary r) {
        return AdminFollowResponse.Summary.of(
                r.id(),
                r.playerId(),
                r.targetPlayerId(),
                r.state(),
                r.muted(),
                r.blocked()
        );
    }

    public static List<AdminFollowResponse.Summary> toSummaries(List<FollowResult.Summary> rs) {
        return rs.stream().map(AdminFollowWebMapper::toSummary).toList();
    }

    public static AdminFollowResponse.Page<AdminFollowResponse.Summary> toSummaryPage(FollowResult.Page<FollowResult.Summary> p) {
        return AdminFollowResponse.Page.of(
                toSummaries(p.contents()),
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages()
        );
    }
}
