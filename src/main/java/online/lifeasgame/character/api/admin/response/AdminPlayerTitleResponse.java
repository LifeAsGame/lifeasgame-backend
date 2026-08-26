package online.lifeasgame.character.api.admin.response;

import java.time.Instant;
import java.util.List;

public final class AdminPlayerTitleResponse {

    private AdminPlayerTitleResponse() {
    }

    public record Infos(Long playerId, List<Info> infos) {
    }

    public record Info(
            Long titleId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
    }

    public record Granted(
            Long playerId,
            Long titleId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
    }

    public record Revoked(Long playerId, Long titleId) {}
}
