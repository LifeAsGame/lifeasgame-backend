package online.lifeasgame.quest.application.result;

import online.lifeasgame.quest.domain.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public final class QuestResult {

    private QuestResult() {}

    public record Blueprint(
            String code,
            String title,
            String category,
            String descriptionMd,
            QuestTarget target,
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
        public static Blueprint from(QuestBlueprint blueprint) {
            return new Blueprint(
                    blueprint.code().value(),
                    blueprint.title().value(),
                    QuestResult.category(blueprint.category()),
                    blueprint.descriptionMd(),
                    blueprint.target(),
                    blueprint.repeatRule().name(),
                    blueprint.completionPolicy().name(),
                    blueprint.definitionVersion(),
                    blueprint.rewardProfileCodeOrNull(),
                    blueprint.usesRewardProfile()
                            ? null
                            : blueprint.reward().exp(),
                    blueprint.usesRewardProfile()
                            ? null
                            : blueprint.reward().stats().stats(),
                    blueprint.dueAt(),
                    blueprint.semanticCategory() == null
                            ? null
                            : blueprint.semanticCategory().name(),
                    blueprint.progressSource() == null
                            ? null
                            : blueprint.progressSource().name(),
                    blueprint.repeatPolicy() == null
                            ? null
                            : blueprint.repeatPolicy().name(),
                    blueprint.roleTemplateCodeOrNull()
            );
        }
    }

    public record Acceptance(
            Long id,
            Long questId,
            Long playerId,
            String code,
            String title,
            String category,
            String descriptionMd,
            QuestTargetType targetType,
            int targetValue,
            int progressValue,
            String status,
            String completionPolicy,
            String repeatRule,
            LocalDate periodStart,
            LocalDate periodEnd,
            Instant acceptedAt,
            String periodKey,
            Instant goalReachedAt,
            Instant completedAt,
            Instant dueAt,
            String semanticCategory,
            String progressSource,
            String repeatPolicy,
            String roleTemplateCode
    ) {
        public static Acceptance from(QuestAcceptance acceptance, Quest quest) {
            return new Acceptance(
                    acceptance.getId(),
                    acceptance.getQuestId(),
                    acceptance.getPlayerId(),
                    quest.getCode(),
                    quest.getTitle().value(),
                    QuestResult.category(quest.getCategory()),
                    quest.getDescriptionMd(),
                    quest.target().type(),
                    quest.target().value(),
                    acceptance.getProgressValue(),
                    acceptance.getStatus().name(),
                    quest.getCompletionPolicy().name(),
                    quest.getRepeatRule().name(),
                    acceptance.getPeriod().start(),
                    acceptance.getPeriod().end(),
                    acceptance.getAcceptedAt(),
                    acceptance.getPeriodKey(),
                    acceptance.getGoalReachedAt(),
                    acceptance.getCompletedAt(),
                    quest.getDueAt(),
                    QuestResult.semanticCategory(quest),
                    QuestResult.progressSource(quest),
                    QuestResult.repeatPolicy(quest),
                    quest.roleTemplateCodeOrNull()
            );
        }
    }

    public record Definition(
            Long id,
            String code,
            String title,
            String category,
            String descriptionMd,
            QuestTargetType targetType,
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
        public static Definition from(Quest quest) {
            return new Definition(
                    quest.getId(),
                    quest.getCode(),
                    quest.getTitle().value(),
                    QuestResult.category(quest.getCategory()),
                    quest.getDescriptionMd(),
                    quest.target().type(),
                    quest.target().value(),
                    quest.getRepeatRule().name(),
                    quest.getCompletionPolicy().name(),
                    quest.getDefinitionVersion(),
                    quest.rewardProfileCodeOrNull(),
                    legacyRewardExp(quest),
                    legacyRewardStats(quest),
                    quest.getDueAt(),
                    QuestResult.semanticCategory(quest),
                    QuestResult.progressSource(quest),
                    QuestResult.repeatPolicy(quest),
                    quest.roleTemplateCodeOrNull()
            );
        }
    }

    public record PlayerQuest(
            String code,
            String title,
            String category,
            String descriptionMd,
            QuestTargetType targetType,
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
        public static PlayerQuest from(Quest quest, QuestAcceptance acceptance) {
            return new PlayerQuest(
                    quest.getCode(),
                    quest.getTitle().value(),
                    QuestResult.category(quest.getCategory()),
                    quest.getDescriptionMd(),
                    quest.target().type(),
                    quest.target().value(),
                    quest.getRepeatRule().name(),
                    quest.getCompletionPolicy().name(),
                    quest.getDefinitionVersion(),
                    quest.rewardProfileCodeOrNull(),
                    legacyRewardExp(quest),
                    legacyRewardStats(quest),
                    quest.getDueAt(),
                    acceptance == null
                            ? null
                            : Acceptance.from(acceptance, quest),
                    QuestResult.semanticCategory(quest),
                    QuestResult.progressSource(quest),
                    QuestResult.repeatPolicy(quest),
                    quest.roleTemplateCodeOrNull()
            );
        }
    }

    public record Canceled(
            Long playerId,
            Long questId,
            String questCode
    ) {
    }

    private static Integer legacyRewardExp(Quest quest) {
        return quest.isLegacyInlineReward() ? quest.getReward().exp() : null;
    }

    private static Map<String, Integer> legacyRewardStats(Quest quest) {
        return quest.isLegacyInlineReward()
                ? quest.getReward().stats().stats()
                : null;
    }

    private static String semanticCategory(Quest quest) {
        return quest.getSemanticCategory() == null
                ? null
                : quest.getSemanticCategory().name();
    }

    private static String progressSource(Quest quest) {
        return quest.getProgressSource() == null
                ? null
                : quest.getProgressSource().name();
    }

    private static String repeatPolicy(Quest quest) {
        return quest.repeatPolicyOrNull() == null
                ? null
                : quest.repeatPolicyOrNull().name();
    }

    private static String category(QuestCategory category) {
        return category == null ? null : category.name();
    }
}
