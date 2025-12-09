package online.lifeasgame.lifelog.api.admin.response;


import java.time.Instant;
import java.util.Set;

public final class AdminCollectionResponse {

    private AdminCollectionResponse() {
    }

    public record Info(
            Long id,
            Long playerId,
            String category,
            String title,
            String originalTitle,
            Integer quantity,
            String conditionNote,
            String acquiredFrom,
            Set<String> tags,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Created(Long id) {
    }
}
