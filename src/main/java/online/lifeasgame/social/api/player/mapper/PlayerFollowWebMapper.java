package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.request.PlayerFollowRequest;
import online.lifeasgame.social.api.player.response.PlayerFollowResponse;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.FollowResult;

import java.util.List;

public final class PlayerFollowWebMapper {

    private PlayerFollowWebMapper() {
    }

    public static PlayerFollowResponse.Page<PlayerFollowResponse.Summary> toSummaryPage(
            FollowResult.Page<FollowResult.Summary> result
    ) {
        return new PlayerFollowResponse.Page<>(
                toSummaries(result.contents()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public static List<PlayerFollowResponse.Summary> toSummaries(List<FollowResult.Summary> results) {
        return results.stream()
                .map(PlayerFollowWebMapper::toSummary)
                .toList();
    }

    public static PlayerFollowResponse.Summary toSummary(FollowResult.Summary result) {
        return new PlayerFollowResponse.Summary(
                result.id(),
                result.playerId(),
                result.targetPlayerId(),
                result.state(),
                result.muted(),
                result.blocked()
        );
    }

    public static FollowCommand.Create toCreateCommand(PlayerFollowRequest.Create request) {
        return new FollowCommand.Create(request.targetPlayerId());
    }

    public static PlayerFollowResponse.Info toInfo(FollowResult.Info result) {
        return new PlayerFollowResponse.Info(
                result.id(),
                result.playerId(),
                result.targetPlayerId(),
                result.state(),
                result.muted(),
                result.blocked(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
