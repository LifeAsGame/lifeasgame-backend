package online.lifeasgame.lifelog.application.result;

import online.lifeasgame.lifelog.domain.CollectionLog;

import java.time.Instant;
import java.util.Set;

public final class CollectionResult {

    private CollectionResult() {
    }

    public record Created(Long id) {
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
        public static Info from(CollectionLog log) {
            return new Info(
                    log.getId(),
                    log.getPlayerId(),
                    log.getCategory().name(),
                    log.getTitle().value(),
                    log.getTitle().original(),
                    log.getQuantity().value(),
                    log.getConditionNote(),
                    log.getAcquiredFrom(),
                    log.getTags().values(),
                    log.getCreatedAt(),
                    log.getUpdatedAt()
            );
        }
    }

    public record Deleted(
            Long playerId,
            Long collectionId
    ) {
    }
}
