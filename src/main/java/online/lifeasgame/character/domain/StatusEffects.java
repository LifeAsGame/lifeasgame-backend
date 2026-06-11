package online.lifeasgame.character.domain;

import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

@EqualsAndHashCode
public final class StatusEffects {

    private final EnumSet<StatusEffectCode> effects;

    private StatusEffects(EnumSet<StatusEffectCode> effects) {
        this.effects = effects.isEmpty()
                ? EnumSet.noneOf(StatusEffectCode.class)
                : EnumSet.copyOf(effects);
    }

    public static StatusEffects empty() {
        return new StatusEffects(EnumSet.noneOf(StatusEffectCode.class));
    }

    public static StatusEffects of(Collection<StatusEffectCode> codes) {
        return new StatusEffects(
                codes.isEmpty() ?
                        EnumSet.noneOf(StatusEffectCode.class) : EnumSet.copyOf(
                                codes instanceof EnumSet ? (EnumSet<StatusEffectCode>) codes : EnumSet.copyOf(codes)
                )
        );
    }

    public List<StatusEffectCode> asList() {
        return List.copyOf(effects);
    }

    public StatusEffects merged(StatusEffects delta) {
        EnumSet<StatusEffectCode> statusEffectCodes = EnumSet.copyOf(this.effects);
        statusEffectCodes.addAll(delta.effects);
        return new StatusEffects(statusEffectCodes);
    }
}
