package online.lifeasgame.quest.application.command;

import online.lifeasgame.quest.domain.QuestStatus;

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

    public record Acceptances(String questCode, QuestStatus status) {
    }

    public record Acceptance(Long acceptanceId) {
    }

    public record PlayerQuests(QuestStatus status) {
    }

    public record PlayerQuest(String questCode) {
    }
}
