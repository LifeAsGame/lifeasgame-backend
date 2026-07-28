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
            String completionPolicy,
            int definitionVersion,
            String rewardProfileCode,
            Integer rewardExp,
            Map<String, Integer> rewardStats,
            Instant dueAt,
            String semanticCategory,
            String progressSource,
            String repeatPolicy,
            String roleTemplateCode
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
            int progressValue,
            String status,
            String completionPolicy,
            String repeatRule,
            LocalDate periodStart,
            LocalDate periodEnd,
            Instant goalReachedAt,
            Instant completedAt,
            Instant dueAt,
            String semanticCategory,
            String progressSource,
            String repeatPolicy,
            String roleTemplateCode
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
            String completionPolicy,
            int definitionVersion,
            String rewardProfileCode,
            Integer rewardExp,
            Map<String, Integer> rewardStats,
            Instant dueAt,
            Acceptance acceptance,
            String semanticCategory,
            String progressSource,
            String repeatPolicy,
            String roleTemplateCode
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
            List<String> statuses,
            List<String> semanticCategories,
            List<String> progressSources,
            List<String> repeatPolicies
    ) {
        public Meta(
                List<String> categories,
                List<String> targetTypes,
                List<String> repeatRules,
                List<String> statuses
        ) {
            this(
                    categories,
                    targetTypes,
                    repeatRules,
                    statuses,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
    }

    public record Canceled(
            Long playerId,
            Long questId,
            String questCode
    ) {
    }
}
