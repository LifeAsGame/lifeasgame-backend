package online.lifeasgame.lifelog.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.lifelog.domain.error.LifeLogError;

import java.util.List;

public enum WatchStatus {
    PLANNED, WATCHING, COMPLETED, DROPPED, ON_HOLD;

    public static WatchStatus parse(String raw) {
        return EnumParsers.parseStrict(
                WatchStatus.class,
                raw,
                LifeLogError.INVALID_WATCH_STATUS,
                "WatchStatus"
        );
    }

    public static WatchStatus parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<WatchStatus> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                WatchStatus.class,
                raw,
                LifeLogError.INVALID_WATCH_STATUS,
                "WatchStatus"
        );
    }
}
