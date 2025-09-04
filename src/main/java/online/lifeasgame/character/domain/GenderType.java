package online.lifeasgame.character.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;

public enum GenderType {
    MALE,
    FEMALE,
    TRANSFORMER,
    LAUNDRY,
    SPONGEBOB;

    private static final Map<String, GenderType> ALIAS = Map.ofEntries(
            Map.entry("M", MALE), Map.entry("MALE", MALE),
            Map.entry("남", MALE), Map.entry("남성", MALE),
            Map.entry("F", FEMALE), Map.entry("FEMALE", FEMALE),
            Map.entry("여", FEMALE), Map.entry("여성", FEMALE),
            Map.entry("TRANSFORMER", TRANSFORMER), Map.entry("TF", TRANSFORMER),
            Map.entry("LAUNDRY", LAUNDRY),
            Map.entry("SPONGEBOB", SPONGEBOB)
    );

    private static final Set<GenderType> STRICT_ALLOWED = Set.of(MALE, FEMALE);

    public static GenderType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(PlayerError.INVALID_GENDER, "gender is blank");
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        GenderType g = ALIAS.getOrDefault(key, null);
        if (g != null) return g;

        try {
            return GenderType.valueOf(key);
        }
        catch (IllegalArgumentException e) {
            throw new DomainException(PlayerError.INVALID_GENDER, "gender=" + raw);
        }
    }

    public static GenderType parseStrict(String raw) {
        GenderType g = parse(raw);
        if (!STRICT_ALLOWED.contains(g)) {
            throw new DomainException(PlayerError.INVALID_GENDER, "gender=" + raw);
        }
        return g;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static GenderType fromJson(String s) { return parse(s); }

    @JsonValue
    public String toJson() { return name().toLowerCase(Locale.ROOT); }
}
