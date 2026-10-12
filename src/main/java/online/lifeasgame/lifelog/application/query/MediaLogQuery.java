package online.lifeasgame.lifelog.application.query;

public final class MediaLogQuery {

    private MediaLogQuery() {
    }

    public record Search(
            String category,
            String status,
            String titleLike,
            int page,
            int size,
            Long personalCategoryId,
            boolean unclassified
    ) {
        public Search(String category, String status, String titleLike, int page, int size) {
            this(category, status, titleLike, page, size, null, false);
        }
    }
}
