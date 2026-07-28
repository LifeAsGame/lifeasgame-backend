package online.lifeasgame.quest.application.command;

import java.time.Instant;
import java.util.Map;

public final class QuestCommand {

    private QuestCommand() {
    }

    public record EnsureDefinition(String code) {
    }

    public record Definition(String questCode) {
    }

    public record UpdateDefinition(
            String questCode,
            Integer definitionVersion,
            String targetType,
            Integer targetValue,
            String rewardProfileCode,
            Integer rewardExp,
            Map<String, Integer> rewardStats,
            String repeatRule,
            Instant dueAt,
            String semanticCategory,
            String progressSource,
            String repeatPolicy,
            String roleTemplateCode
    ) {
        public UpdateDefinition(
                String questCode,
                Integer definitionVersion,
                String targetType,
                Integer targetValue,
                String rewardProfileCode,
                Integer rewardExp,
                Map<String, Integer> rewardStats,
                String repeatRule,
                Instant dueAt
        ) {
            this(
                    questCode,
                    definitionVersion,
                    targetType,
                    targetValue,
                    rewardProfileCode,
                    rewardExp,
                    rewardStats,
                    repeatRule,
                    dueAt,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public record Acceptances(String questCode, String status) {
    }

    public record Acceptance(Long acceptanceId) {
    }

    public record PlayerQuests(String status) {
    }

    public record PlayerQuest(String questCode) {
    }

    public record Accept(
            String questCode,
            Long partyId,
            Long guildId
    ) {
    }

    public record Cancel(
            String questCode,
            String reason
    ) {
    }

    public record AdjustProgress(
            Integer delta
    ) {
    }

    public record ChangeStatus(
            String status,
            String reason
    ) {
    }
}
