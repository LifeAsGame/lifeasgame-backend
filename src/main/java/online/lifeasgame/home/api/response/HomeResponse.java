package online.lifeasgame.home.api.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class HomeResponse {

    private HomeResponse() {
    }

    public record Summary(
            Instant generatedAt,
            List<JournalEntry> recentJournal,
            List<RecentAchievement> recentAchievements,
            Journey journey,
            RoleActivity roleActivity30d
    ) {
    }

    public record JournalEntry(
            Long lifeLogId,
            String sourceType,
            String subtype,
            String entryMode,
            Long primaryRoleId,
            Long roleEventId,
            Instant recordedAt,
            Preview preview
    ) {
    }

    public record RecentAchievement(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
    }

    public sealed interface Preview permits
            CollectionPreview,
            ExercisePreview,
            MediaPreview {
    }

    public record CollectionPreview(
            String category,
            String title,
            Integer quantity
    ) implements Preview {
    }

    public record ExercisePreview(
            String category,
            Integer durationMinutes,
            Double distanceKm,
            Integer calories,
            LocalDate exercisedOn,
            String memo
    ) implements Preview {
    }

    public record MediaPreview(
            String category,
            String title,
            Integer currentEpisode,
            Integer totalEpisode,
            String status,
            Double rating
    ) implements Preview {
    }

    public record Journey(
            List<CurrentQuest> currentQuests,
            List<SelectedRoute> selectedRoutes
    ) {
    }

    public record CurrentQuest(
            Long acceptanceId,
            String questCode,
            String title,
            String status,
            int progressValue,
            int targetValue,
            Instant acceptedAt,
            Instant goalReachedAt
    ) {
    }

    public record SelectedRoute(
            Long routeId,
            String routeCode,
            String title,
            String status,
            Long currentStepId,
            Instant selectedAt,
            Instant completedAt
    ) {
    }

    public record RoleActivity(
            Instant windowStart,
            Instant windowEnd,
            long totalRecords,
            long assignedRecords,
            long unassignedRecords,
            List<RoleBucket> roles
    ) {
    }

    public record RoleBucket(
            Long roleId,
            String roleName,
            long recordCount,
            double share
    ) {
    }
}
