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
            Instant dueAt
    ) {
        public static Blueprint from(QuestBlueprint blueprint) {
            return new Blueprint(
                    blueprint.code().value(),
                    blueprint.title().value(),
                    blueprint.category().name(),
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
                    blueprint.dueAt()
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
            Instant goalReachedAt,
            Instant completedAt,
            Instant dueAt
    ) {
        public static Acceptance from(QuestAcceptance acceptance, Quest quest) {
            return new Acceptance(
                    acceptance.getId(),
                    acceptance.getQuestId(),
                    acceptance.getPlayerId(),
                    quest.getCode(),
                    quest.getTitle().value(),
                    quest.getCategory().name(),
                    quest.getDescriptionMd(),
                    quest.target().type(),
                    quest.target().value(),
                    acceptance.getProgressValue(),
                    acceptance.getStatus().name(),
                    quest.getCompletionPolicy().name(),
                    quest.getRepeatRule().name(),
                    acceptance.getPeriod().start(),
                    acceptance.getPeriod().end(),
                    acceptance.getGoalReachedAt(),
                    acceptance.getCompletedAt(),
                    quest.getDueAt()
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
            Instant dueAt
    ) {
        public static Definition from(Quest quest) {
            return new Definition(
                    quest.getId(),
                    quest.getCode(),
                    quest.getTitle().value(),
                    quest.getCategory().name(),
                    quest.getDescriptionMd(),
                    quest.target().type(),
                    quest.target().value(),
                    quest.getRepeatRule().name(),
                    quest.getCompletionPolicy().name(),
                    quest.getDefinitionVersion(),
                    quest.rewardProfileCodeOrNull(),
                    legacyRewardExp(quest),
                    legacyRewardStats(quest),
                    quest.getDueAt()
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
            Acceptance acceptance
    ) {
        public static PlayerQuest from(Quest quest, QuestAcceptance acceptance) {
            return new PlayerQuest(
                    quest.getCode(),
                    quest.getTitle().value(),
                    quest.getCategory().name(),
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
                    acceptance == null ? null : Acceptance.from(acceptance, quest)
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
}
