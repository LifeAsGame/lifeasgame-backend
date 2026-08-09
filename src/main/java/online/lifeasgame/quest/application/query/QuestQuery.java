package online.lifeasgame.quest.application.query;

public final class QuestQuery {

    private QuestQuery() {
    }

    public record Definition(String questCode) {
    }

    public record Acceptances(String questCode, String status) {
    }

    public record Acceptance(Long acceptanceId) {
    }

    public record PlayerQuests(String status) {
    }

    public record PlayerQuest(String questCode) {
    }
}
