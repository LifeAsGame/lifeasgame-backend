package online.lifeasgame.lifelog.domain.record;

public enum LifeLogReflectionScope {
    WEEKLY_LOOKBACK;

    public static LifeLogReflectionScope parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "reflectionScope must be an official scope",
                    exception
            );
        }
    }
}
