package online.lifeasgame.character.domain;

import java.util.Locale;

public enum StatusEffectCode {
    POISON(StatusEffectCategory.DEBUFF),
    STUN(StatusEffectCategory.CROWD_CONTROL),
    REGEN(StatusEffectCategory.HOT),
    BURN(StatusEffectCategory.DOT),
    FREEZE(StatusEffectCategory.CROWD_CONTROL),
    ;

    private final StatusEffectCategory category;

    StatusEffectCode(StatusEffectCategory category) {
        this.category = category;
    }

    public StatusEffectCategory category() {
        return category;
    }

    public static StatusEffectCode parse(String raw) {
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown status effect code: " + raw);
        }
    }
}
