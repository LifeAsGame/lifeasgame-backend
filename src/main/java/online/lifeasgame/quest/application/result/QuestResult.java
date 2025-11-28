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
            int rewardExp,
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
                    blueprint.reward().exp(),
                    blueprint.reward().stats().stats(),
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
            int progress,
            String status,
            String repeatRule,
            LocalDate periodStart,
            LocalDate periodEnd,
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
                    quest.getRepeatRule().name(),
                    acceptance.getPeriod().start(),
                    acceptance.getPeriod().end(),
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
            int rewardExp,
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
                    quest.getReward().exp(),
                    quest.getReward().stats().stats(),
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
            int rewardExp,
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
                    quest.getReward().exp(),
                    quest.getReward().stats().stats(),
                    quest.getDueAt(),
                    acceptance == null ? null : Acceptance.from(acceptance, quest)
            );
        }
    }
}
