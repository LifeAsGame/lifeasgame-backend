package online.lifeasgame.social.api.admin.mapper;

import online.lifeasgame.social.api.admin.request.AdminFollowRequest;
import online.lifeasgame.social.api.admin.response.AdminFollowResponse;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.FollowResult;

import java.util.List;

public final class AdminFollowWebMapper {
    
    private AdminFollowWebMapper() {
    }

    public static FollowCommand.Create toCreateCommand(AdminFollowRequest.Create request) {
        return new FollowCommand.Create(request.targetPlayerId());
    }

    public static AdminFollowResponse.Summary toSummary(FollowResult.Summary result) {
        return new AdminFollowResponse.Summary(
                result.id(),
                result.playerId(),
                result.targetPlayerId(),
                result.state(),
                result.muted(),
                result.blocked()
        );
    }

    public static List<AdminFollowResponse.Summary> toSummaries(List<FollowResult.Summary> results) {
        return results.stream()
                .map(AdminFollowWebMapper::toSummary)
                .toList();
    }

    public static AdminFollowResponse.Page<AdminFollowResponse.Summary> toSummaryPage(
            FollowResult.Page<FollowResult.Summary> p
    ) {
        return new AdminFollowResponse.Page<>(
                toSummaries(p.contents()),
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages()
        );
    }
}
