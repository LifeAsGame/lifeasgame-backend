package online.lifeasgame.quest.domain.seed;

public enum QuestContentPeriodBoundary {
    PLAYER_LIFETIME("PLAYER_LIFETIME"),
    FROM_ACCEPTANCE_UNTIL_COMPLETION(
            "FROM_ACCEPTANCE_UNTIL_COMPLETION"
    ),
    MONDAY_00_00_INCLUSIVE_TO_NEXT_MONDAY_00_00_EXCLUSIVE(
            "MONDAY_00:00_INCLUSIVE_TO_NEXT_MONDAY_00:00_EXCLUSIVE"
    ),
    LOCAL_DAY_00_00_TO_NEXT_DAY_00_00(
            "LOCAL_DAY_00:00_TO_NEXT_DAY_00:00"
    );

    private final String value;

    QuestContentPeriodBoundary(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
