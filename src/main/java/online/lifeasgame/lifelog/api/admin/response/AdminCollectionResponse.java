package online.lifeasgame.lifelog.api.admin.response;


import java.time.Instant;
import java.util.List;
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

    public record Infos(List<Info> items) {
    }

    public record Created(Long id) {
    }

    public record Deleted(Long id) {
    }

    public record Page<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
