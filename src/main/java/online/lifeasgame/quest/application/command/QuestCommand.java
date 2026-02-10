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
            String targetType,
            Integer targetValue,
            Integer rewardExp,
            Map<String, Integer> rewardStats,
            String repeatRule,
            Instant dueAt
    ) {
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
}
