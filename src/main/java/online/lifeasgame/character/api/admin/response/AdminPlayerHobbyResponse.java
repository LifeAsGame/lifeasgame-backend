package online.lifeasgame.character.api.admin.response;

import java.time.LocalDate;

public final class AdminPlayerHobbyResponse {

    private AdminPlayerHobbyResponse() {
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
}
