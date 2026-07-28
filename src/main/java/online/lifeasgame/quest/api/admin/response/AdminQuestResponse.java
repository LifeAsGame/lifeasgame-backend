package online.lifeasgame.quest.api.admin.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminQuestResponse {

    private AdminQuestResponse() {
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

    public record Definition(
            Long id,
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

    public record Definitions(List<Definition> definitions) {
    }

    public record Acceptance(
            Long id,
            Long questId,
            Long playerId,
            String code,
            String title,
            String category,
            String targetType,
            int targetValue,
            int progressValue,
            String status,
            String completionPolicy,
            String repeatRule,
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd,
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

    public record RewardTriggered(
            Long acceptanceId,
            String questCode,
            Long playerId,
            String state,
            String correlationId,
            Instant triggeredAt
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
}
