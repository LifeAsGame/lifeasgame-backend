package online.lifeasgame.lifelog.application.command;

import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;

import java.util.Set;

public final class CollectionCommand {

    private CollectionCommand() {
    }

    public record Create(
            String category,
            String title,
            String originalTitle,
            Integer quantity,
            String conditionNote,
            String acquiredFrom,
            Set<String> tags,
            LifeLogRecordMetadataCommand lifeLogMetadata
    ) {
        public Create(
                String category,
                String title,
                String originalTitle,
                Integer quantity,
                String conditionNote,
                String acquiredFrom,
                Set<String> tags
        ) {
            this(
                    category,
                    title,
                    originalTitle,
                    quantity,
                    conditionNote,
                    acquiredFrom,
                    tags,
                    LifeLogRecordMetadataCommand.none()
            );
        }
    }

    public record Update(
            Integer quantity,
            String conditionNote,
            String acquiredFrom
    ) {
    }

    public record Search(
            String category,
            String titleLike,
            int page,
            int size
    ) {
    }
}
