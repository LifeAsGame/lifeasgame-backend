package online.lifeasgame.lifelog.application.query;

public final class MediaLogQuery {

    private MediaLogQuery() {
    }

    public record Search(
            String category,
            String status,
            String titleLike,
            int page,
            int size
    ) {
    }
}
