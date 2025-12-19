package online.lifeasgame.character.domain;

import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.lang.EnumParsers;

import java.util.List;

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
        return EnumParsers.parseStrict(
                StatusEffectCode.class,
                raw,
                PlayerError.INVALID_STATUS_EFFECT_CODE,
                "Status effect code"
        );
    }

    public static List<StatusEffectCode> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                StatusEffectCode.class,
                raw,
                PlayerError.INVALID_STATUS_EFFECT_CODE,
                "Status effect codes"
        );
    }
}
