package online.lifeasgame.social.application.result;

import online.lifeasgame.social.domain.Follow;

import java.time.Instant;
import java.util.List;

public final class FollowResult {

    public record Info(
            Long id,
            Long playerId,
            Long targetPlayerId,
            String state,
            boolean muted,
            boolean blocked,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Info from(Follow f) {
            return new Info(
                    f.getId(),
                    f.getPlayerId(),
                    f.getTargetPlayerId(),
                    f.getState().name(),
                    f.isMuted(),
                    f.isBlocked(),
                    f.getCreatedAt(),
                    f.getUpdatedAt()
            );
        }
    }

    public record Summary(Long id, Long playerId, Long targetPlayerId, String state, boolean muted, boolean blocked) {
        public static Summary from(Follow f) {
            return new Summary(
                    f.getId(),
                    f.getPlayerId(),
                    f.getTargetPlayerId(),
                    f.getState().name(),
                    f.isMuted(),
                    f.isBlocked()
            );
        }
    }

    public record Page<T>(List<T> contents, int page, int size, long totalElements, int totalPages) {
        public static <T> Page<T> of(List<T> contents, int page, int size, long total) {
            int totalPages = (int) Math.ceil(total / (double) Math.max(size, 1));
            return new Page<>(contents, page, size, total, totalPages);
        }
    }
}

