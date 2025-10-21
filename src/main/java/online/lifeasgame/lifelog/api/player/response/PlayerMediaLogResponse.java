package online.lifeasgame.lifelog.api.player.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public final class PlayerMediaLogResponse {

    private PlayerMediaLogResponse() {
    }

    public record Created(Long id) {
    }

    public record Info(
            Long id,
            Long playerId,
            String category,
            String title,
            String originalTitle,
            int currentEpisode,
            int totalEpisode,
            String status,
            Double rating,
            Set<String> tags,
            int rewatchCount,
            LocalDate startedOn,
            LocalDate finishedOn,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
