package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mana {

    @Column(name = "mp_cur", nullable = false)
    private int current;

    @Column(name = "mp_cap", nullable = false)
    private int cap;

    private Mana(int current, int cap) {
        Guard.minValue(cap, 1, "mp cap");
        this.cap = cap;
        this.current = clamp(current, cap);
    }

    public static Mana full(int cap) {
        return new Mana(cap, cap);
    }

    public static Mana of(int current, int cap) {
        return new Mana(current, cap);
    }

    public Mana withCap(int newCap) {
        return new Mana(Math.min(current, newCap), newCap);
    }

    public boolean canSpend(int amount) {
        Guard.minValue(amount, 0, "spend amount");
        return current >= amount;
    }

    public Mana spend(int amount) {
        Guard.minValue(amount, 0, "spend amount");
        return new Mana(Math.max(0, current - amount), cap);
    }

    public Mana recover(int amount) {
        Guard.minValue(amount, 0, "recover amount");
        return new Mana(Math.min(cap, current + amount), cap);
    }

    public int current() { return current; }
    public int cap() { return cap; }

    private static int clamp(int cur, int cap) {
        return Math.max(0, Math.min(cur, cap));
    }
}
