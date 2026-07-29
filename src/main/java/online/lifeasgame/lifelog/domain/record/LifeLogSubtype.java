package online.lifeasgame.lifelog.domain.record;

public enum LifeLogSubtype {
    QUICK_NOTE,
    ACTIVITY,
    STUDY,
    PROJECT,
    MEMORY,
    REFLECTION,
    MOOD,
    HEALTH_NOTE;

    public static LifeLogSubtype parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "lifeLogSubtype must be an official subtype"
            );
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "lifeLogSubtype must be an official subtype",
                    exception
            );
        }
    }
}
