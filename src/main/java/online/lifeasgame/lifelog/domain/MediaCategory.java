package online.lifeasgame.lifelog.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.lifelog.domain.error.LifeLogError;

import java.util.List;

public enum MediaCategory {
    ANIME, MOVIE, SERIES, BOOK, WEBTOON, GAME, MUSIC;

    public static MediaCategory parse(String raw) {
        return EnumParsers.parseStrict(
                MediaCategory.class,
                raw,
                LifeLogError.INVALID_MEDIA_CATEGORY,
                "Media category"
        );
    }

    public static MediaCategory parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<MediaCategory> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                MediaCategory.class,
                raw,
                LifeLogError.INVALID_MEDIA_CATEGORY,
                "Media categories"
        );
    }
}
