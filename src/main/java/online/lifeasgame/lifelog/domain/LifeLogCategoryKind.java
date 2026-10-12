package online.lifeasgame.lifelog.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.error.LifeLogError;

public enum LifeLogCategoryKind {
    COLLECTION, EXERCISE, MEDIA;

    public static LifeLogCategoryKind parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new DomainException(LifeLogError.INVALID_CATEGORY_KIND);
        }
    }

    public String validateSystemCode(String code) {
        if (code == null) throw new DomainException(LifeLogError.INVALID_SYSTEM_CATEGORY);
        return switch (this) {
            case COLLECTION -> CollectionCategory.parse(code).name();
            case EXERCISE -> ExerciseCategory.parse(code).name();
            case MEDIA -> MediaCategory.parse(code).name();
        };
    }
}
