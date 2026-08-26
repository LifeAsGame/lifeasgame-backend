package online.lifeasgame.character.api.admin.response;

import java.time.LocalDate;
import java.util.List;

public final class AdminPlayerHobbyResponse {

    private AdminPlayerHobbyResponse() {
    }

    public record Infos(Long playerId, List<Info> infos) {
    }

    public record Info(
            Long hobbyId,
            String name,
            String category,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
    }

    public record Granted(
            Long playerId,
            Long hobbyId,
            String name,
            String category,
            String customName,
            String detail,
            int proficiency,
            String status,
            LocalDate startedOn,
            long xp
    ) {
    }

    public record Revoked(Long playerId, Long hobbyId) {}
}
