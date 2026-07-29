package online.lifeasgame.quest.domain;

import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.List;

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
    INVENTORY_COLLECTOR_100("quest:inventory:collector-100"),
    Q_RECORD_FIRST_TRACE("Q_RECORD_FIRST_TRACE"),
    Q_RECORD_THREE_TRACES("Q_RECORD_THREE_TRACES"),
    Q_RECORD_WEEKLY_LOOKBACK("Q_RECORD_WEEKLY_LOOKBACK"),
    Q_GROWTH_ONE_FOCUS("Q_GROWTH_ONE_FOCUS"),
    Q_RECOVERY_REST_TEN("Q_RECOVERY_REST_TEN");

    private final String value;

    QuestCode(String value) {
        this.value = Guard.notBlank(value, "questCode");
    }

    public String value() {
        return value;
    }

    public static QuestCode parse(String raw) {
        return EnumParsers.parseStrict(
                QuestCode.class,
                raw,
                QuestError.INVALID_QUEST_CODE,
                "Quest code"
        );
    }

    public static QuestCode parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<QuestCode> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                QuestCode.class,
                raw,
                QuestError.INVALID_QUEST_CODE,
                "Quest codes"
        );
    }
}
