package online.lifeasgame.role.application.result;

import java.time.Instant;
import java.util.List;

public final class RoleScheduleResult {
    private RoleScheduleResult() {}

    public record Row(String sourceType, Long sourceId, Long roleId, Long guildId,
                      String guildName, String title, Instant startsAt, Instant endsAt,
                      String status, Boolean myRsvp) {}

    public record Page(List<Row> contents, int page, int size, long totalElements, int totalPages) {
        public static Page of(List<Row> rows, int page, int size, long total) {
            return new Page(rows, page, size, total, Math.toIntExact((total + size - 1) / size));
        }
    }
}
