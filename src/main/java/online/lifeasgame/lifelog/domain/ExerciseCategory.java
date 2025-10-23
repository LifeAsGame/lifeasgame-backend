package online.lifeasgame.lifelog.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.lifelog.domain.error.LifeLogError;

public enum ExerciseCategory {
    RUNNING, WALKING, CYCLING, SWIMMING, GYM, YOGA, OTHER;

    public static ExerciseCategory parse(String raw) {
        return EnumParsers.parseStrict(
                ExerciseCategory.class,
                raw,
                LifeLogError.INVALID_EXERCISE_CATEGORY,
                "Exercise Category"
        );
    }

    public static ExerciseCategory parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
