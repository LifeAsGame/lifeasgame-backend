package online.lifeasgame.character.domain;

import java.util.List;
import lombok.EqualsAndHashCode;
import online.lifeasgame.core.guard.Guard;

@EqualsAndHashCode
public final class StatusEffects {

    private final List<String> effects;

    private StatusEffects(List<String> effects) {
        this.effects = List.copyOf(effects);
    }

    public static StatusEffects empty() {
        return new StatusEffects(List.of());
    }

    public static StatusEffects of(List<String> raw) {
        Guard.notNull(raw, "effects");
        return new StatusEffects(raw);
    }

    public List<String> asList() { return effects; }
}
