package online.lifeasgame.quest.api.player.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class QuestResponse {

    private QuestResponse() {
    }

    public record Blueprint(
            String code,
            String title,
            String category,
            String descriptionMd,
            String targetType,
            int targetValue,
            String repeatRule,
            int rewardExp,
            Map<String, Integer> rewardStats,
            Instant dueAt
    ) {
    }

    public record Blueprints(List<Blueprint> blueprints) {
    }

    public record Acceptance(
            Long id,
            Long questId,
            String code,
            String title,
            String category,
            String descriptionMd,
            String targetType,
            int targetValue,
            int progress,
            String status,
            String repeatRule,
            LocalDate periodStart,
            LocalDate periodEnd,
            Instant completedAt,
            Instant dueAt
    ) {
    }

    public record Acceptances(List<Acceptance> acceptances) {
    }

    public record PlayerQuest(
            String code,
            String title,
            String category,
            String descriptionMd,
            String targetType,
            int targetValue,
            String repeatRule,
            int rewardExp,
            Map<String, Integer> rewardStats,
            Instant dueAt,
            Acceptance acceptance
    ) {
    }

    public record Accepted(
            boolean newlyCreated,
            Acceptance acceptance
    ) {
    }

    public record Reward(
            int exp,
            Map<String, Integer> stats
    ) {
    }

    public record RewardClaimed(
            String questCode,
            Long acceptanceId,
            String state,
            Reward reward,
            String correlationId,
            Instant requestedAt
    ) {
    }

    public record Meta(
            List<String> categories,
            List<String> targetTypes,
            List<String> repeatRules,
            List<String> statuses
    ) {
    }
}
