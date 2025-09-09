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
        if (amount == 0 || current == 0) {
            return this;
        }

        int next = current - amount;
        if (next < 0) {
            next = 0;
        }

        return new Mana(current, next);
    }

    public Mana recover(int amount) {
        Guard.minValue(amount, 0, "recover mp amount");
        if (amount == 0 || current == cap) {
            return this;
        }

        long sum = (long) current + (long) amount;
        int next = (int) Math.min(cap, sum);
        if (next == current) {
            return this;
        }

        return new Mana(current, next);
    }

    public Mana increaseCap(int amount) {
        Guard.minValue(amount, 0, "increase cap");
        if (amount == 0) {
            return this;
        }

        long sum = (long) cap + (long) amount;
        int next = sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
        if (next == cap) {
            return this;
        }

        return new Mana(current, next);
    }

    public Mana decreaseCap(int amount) {
        Guard.minValue(amount, 0, "decrease cap");
        if (amount == 0) {
            return this;
        }

        int next = cap - amount;
        if (next < 0) {
            next = 0;
        }

        return new Mana(current, next);
    }

    public int current() { return current; }
    public int cap() { return cap; }

    private static int clamp(int cur, int cap) {
        return Math.max(0, Math.min(cur, cap));
    }
}
