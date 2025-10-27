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
