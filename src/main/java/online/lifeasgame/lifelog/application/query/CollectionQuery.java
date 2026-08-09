package online.lifeasgame.lifelog.application.query;

public final class CollectionQuery {

    private CollectionQuery() {
    }

    public record Search(
            String category,
            String titleLike,
            int page,
            int size
    ) {
    }
}
