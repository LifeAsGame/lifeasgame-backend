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
public class Health {

    @Column(name = "hp_cur", nullable = false)
    private int current;

    @Column(name = "hp_cap", nullable = false)
    private int cap;

    private Health(int current, int cap) {
        Guard.minValue(cap, 1, "hpDelta cap");
        this.cap = cap;
        this.current = clamp(current, cap);
    }

    public static Health full(int cap) {
        return new Health(cap, cap);
    }

    public static Health of(int current, int cap) {
        return new Health(current, cap);
    }

    public Health withCap(int newCap) {
        return new Health(Math.min(current, newCap), newCap);
    }

    public Health heal(int amount) {
        Guard.minValue(amount, 0, "heal amount");
        if (amount == 0 || current == cap) {
            return this;
        }

        long sum = (long) current + (long) amount;
        int next = (int) Math.min(cap, sum);
        if (next == current) {
            return this;
        }

        return new Health(next, cap);
    }

    public Health damage(int amount) {
        Guard.minValue(amount, 0, "damage amount");
        if (amount == 0 || current == 0) {
            return this;
        }

        int next = current - amount;
        if (next < 0) {
            next = 0;
        }

        return new Health(next, cap);
    }

    public int current() {
        return current;
    }

    public int cap() {
        return cap;
    }

    private static int clamp(int cur, int cap) {
        return Math.max(0, Math.min(cur, cap));
    }

    public Health increaseCap(int amount) {
        Guard.minValue(amount, 0, "increase cap");
        if (amount == 0) {
            return this;
        }

        long sum = (long) cap + (long) amount;
        int next = sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
        if (next == cap) {
            return this;
        }

        return new Health(current, next);
    }

    public Health decreaseCap(int amount) {
        Guard.minValue(amount, 0, "decrease cap");
        if (amount == 0) {
            return this;
        }

        int next = cap - amount;
        if (next < 0) {
            next = 0;
        }

        return new Health(current, next);
    }
}
