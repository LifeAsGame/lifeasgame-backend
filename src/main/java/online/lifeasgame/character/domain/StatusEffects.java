package online.lifeasgame.character.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import lombok.EqualsAndHashCode;

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
        return new StatusEffects(codes.isEmpty()
                ? EnumSet.noneOf(StatusEffectCode.class)
                : EnumSet.copyOf(codes instanceof EnumSet ? (EnumSet<StatusEffectCode>) codes : EnumSet.copyOf(codes)));
    }

    public List<StatusEffectCode> asList() {
        return List.copyOf(effects);
    }

    public StatusEffects merged(StatusEffects delta) {
        EnumSet<StatusEffectCode> m = EnumSet.copyOf(this.effects);
        m.addAll(delta.effects);
        return new StatusEffects(m);
    }
}
