package online.lifeasgame.lifelog.application.command;

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
            Set<String> tags
    ) {
    }

    public record Update(Integer quantity, String conditionNote, String acquiredFrom) {
    }

    public record Search(String category, String titleLike, int page, int size) {
    }
}
