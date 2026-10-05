package online.lifeasgame.social.application;

import java.time.Instant;
import java.util.List;

public final class GroupActivityResult {
    private GroupActivityResult() {}

    public record Capabilities(boolean canEdit, boolean canManageEditors, boolean canRsvp) {}
    public record Activity(Long id, String groupType, Long groupId, String title, String sharedDescription,
                           String location, Instant startsAt, Instant endsAt, String status,
                           Long createdByPlayerId, Instant createdAt, Instant updatedAt, long version,
                           long participantCount, boolean myRsvp, Capabilities capabilities) {}
    public record Participant(Long playerId, Instant joinedAt) {}
    public record Editor(Long playerId) {}
    public record Page<T>(List<T> contents, int page, int size, long totalElements, int totalPages) {
        public static <T> Page<T> of(List<T> rows, int page, int size, long total) {
            return new Page<>(rows, page, size, total, Math.toIntExact((total + size - 1) / size));
        }
    }
}
