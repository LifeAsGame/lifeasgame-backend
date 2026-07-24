package online.lifeasgame.quest.domain;

public enum QuestCompletionPolicy {
    AUTO,
    USER_CONFIRM;

    public static QuestCompletionPolicy defaultIfNull(QuestCompletionPolicy policy) {
        return policy == null ? AUTO : policy;
    }
}
