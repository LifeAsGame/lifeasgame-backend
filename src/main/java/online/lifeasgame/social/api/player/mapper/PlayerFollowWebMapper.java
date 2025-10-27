package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.request.PlayerFollowRequest;
import online.lifeasgame.social.api.player.response.PlayerFollowResponse;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.FollowResult;

import java.util.List;

public final class PlayerFollowWebMapper {
    private PlayerFollowWebMapper() {
    }

    // Request -> Command
    public static FollowCommand.Create toCommand(PlayerFollowRequest.Create r) {
        return FollowCommand.Create.of(r.targetPlayerId());
    }

    public static FollowCommand.Unfollow toUnfollow(PlayerFollowRequest.Empty r) {
        return FollowCommand.Unfollow.of();
    }

    public static FollowCommand.Mute toMute(PlayerFollowRequest.Empty r) {
        return FollowCommand.Mute.of();
    }

    public static FollowCommand.Unmute toUnmute(PlayerFollowRequest.Empty r) {
        return FollowCommand.Unmute.of();
    }

    public static FollowCommand.Block toBlock(PlayerFollowRequest.Empty r) {
        return FollowCommand.Block.of();
    }

    public static FollowCommand.Unblock toUnblock(PlayerFollowRequest.Empty r) {
        return FollowCommand.Unblock.of();
    }

    // Result -> Response
    public static PlayerFollowResponse.Info toInfo(FollowResult.Info r) {
        return PlayerFollowResponse.Info.of(
                r.id(),
                r.playerId(),
                r.targetPlayerId(),
                r.state(),
                r.muted(),
                r.blocked(),
                r.createdAt(),
                r.updatedAt()
        );
    }

    public static PlayerFollowResponse.Summary toSummary(FollowResult.Summary r) {
        return PlayerFollowResponse.Summary.of(
                r.id(),
                r.playerId(),
                r.targetPlayerId(),
                r.state(),
                r.muted(),
                r.blocked()
        );
    }

    public static List<PlayerFollowResponse.Summary> toSummaries(List<FollowResult.Summary> rs) {
        return rs.stream().map(PlayerFollowWebMapper::toSummary).toList();
    }

    public static PlayerFollowResponse.Page<PlayerFollowResponse.Summary> toSummaryPage(FollowResult.Page<FollowResult.Summary> p) {
        return PlayerFollowResponse.Page.of(
                toSummaries(p.contents()),
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages()
        );
    }
}
