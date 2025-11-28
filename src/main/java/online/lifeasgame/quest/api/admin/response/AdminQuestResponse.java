package online.lifeasgame.quest.api.admin.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminQuestResponse {

    private AdminQuestResponse() {}

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
        public static Blueprint of(
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
            return new Blueprint(code, title, category, descriptionMd, targetType, targetValue, repeatRule, rewardExp, rewardStats, dueAt);
        }
    }

    public record Blueprints(List<Blueprint> blueprints) {
        public static Blueprints of(List<Blueprint> blueprints) {
            return new Blueprints(blueprints);
        }
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
            int rewardExp,
            Map<String, Integer> rewardStats,
            Instant dueAt
    ) {
        public static Definition of(
                Long id,
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
            return new Definition(id, code, title, category, descriptionMd, targetType, targetValue, repeatRule, rewardExp, rewardStats, dueAt);
        }
    }

    public record Definitions(List<Definition> definitions) {
        public static Definitions of(List<Definition> definitions) {
            return new Definitions(definitions);
        }
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
            int progress,
            String status,
            String repeatRule,
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd,
            Instant completedAt,
            Instant dueAt
    ) {
        public static Acceptance of(
                Long id,
                Long questId,
                Long playerId,
                String code,
                String title,
                String category,
                String targetType,
                int targetValue,
                int progress,
                String status,
                String repeatRule,
                java.time.LocalDate periodStart,
                java.time.LocalDate periodEnd,
                Instant completedAt,
                Instant dueAt
        ) {
            return new Acceptance(
                    id,
                    questId,
                    playerId,
                    code,
                    title,
                    category,
                    targetType,
                    targetValue,
                    progress,
                    status,
                    repeatRule,
                    periodStart,
                    periodEnd,
                    completedAt,
                    dueAt
            );
        }
    }

    public record Acceptances(List<Acceptance> acceptances) {
        public static Acceptances of(List<Acceptance> acceptances) {
            return new Acceptances(acceptances);
        }
    }
}
