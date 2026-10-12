package online.lifeasgame.lifelog.application.query;

import java.time.LocalDate;

public final class ExerciseQuery {

    private ExerciseQuery() {
    }

    public record Search(
            String category,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            Long personalCategoryId,
            boolean unclassified
    ) {
        public Search(String category, LocalDate from, LocalDate to, int page, int size) {
            this(category, from, to, page, size, null, false);
        }
    }
}
