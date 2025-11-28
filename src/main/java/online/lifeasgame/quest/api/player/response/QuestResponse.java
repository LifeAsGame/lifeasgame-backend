package online.lifeasgame.quest.api.player.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class QuestResponse {

    private QuestResponse() {}

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
        public static Acceptance of(
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
            return new Acceptance(
                    id,
                    questId,
                    code,
                    title,
                    category,
                    descriptionMd,
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
        public static PlayerQuest of(
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
            return new PlayerQuest(
                    code,
                    title,
                    category,
                    descriptionMd,
                    targetType,
                    targetValue,
                    repeatRule,
                    rewardExp,
                    rewardStats,
                    dueAt,
                    acceptance
            );
        }
    }
}
