package online.lifeasgame.quest.domain;

import online.lifeasgame.core.guard.Guard;

public enum QuestCode {
    PLAYER_WELCOME("quest:player:welcome"),
    PLAYER_LEVEL_TRACK("quest:player:level:progress"),
    PLAYER_LEVEL_MILESTONE_10("quest:player:level:reach10"),
    PLAYER_LEVEL_MILESTONE_20("quest:player:level:reach20"),
    PLAYER_LEVEL_MILESTONE_30("quest:player:level:reach30"),
    PLAYER_LEVEL_MILESTONE_40("quest:player:level:reach40"),
    PLAYER_LEVEL_MILESTONE_50("quest:player:level:reach50"),
    PLAYER_LEVEL_MILESTONE_60("quest:player:level:reach60"),
    PLAYER_LEVEL_MILESTONE_70("quest:player:level:reach70"),
    PLAYER_LEVEL_MILESTONE_80("quest:player:level:reach80"),
    PLAYER_LEVEL_MILESTONE_90("quest:player:level:reach90"),
    PLAYER_LEVEL_MILESTONE_100("quest:player:level:reach100"),
    EXERCISE_MINUTES_300("quest:exercise:minutes-300"),
    COLLECTION_HUNTER_10("quest:collection:hunter-10"),
    MEDIA_BINGE_5("quest:media:binge-5"),
    INVENTORY_COLLECTOR_100("quest:inventory:collector-100");

    private final String value;

    QuestCode(String value) {
        this.value = Guard.notBlank(value, "questCode");
    }

    public String value() {
        return value;
    }

    public static QuestCode fromValue(String value) {
        Guard.notBlank(value, "questCode");
        for (QuestCode code : values()) {
            if (code.value.equals(value)) {
                return code;
            }
        }
        throw new IllegalArgumentException("Unknown quest code: " + value);
    }
}
