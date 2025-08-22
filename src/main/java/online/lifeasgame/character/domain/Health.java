package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Health {

    @Column(name = "hp_cur", nullable = false)
    private int current;

    @Column(name = "hp_cap", nullable = false)
    private int cap;

    private Health(int current, int cap) {
        Guard.minValue(cap, 1, "hp cap");
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
        return new Health(Math.min(cap, current + amount), cap);
    }

    public Health damage(int amount) {
        Guard.minValue(amount, 0, "damage amount");
        return new Health(Math.max(0, current - amount), cap);
    }

    public int current() { return current; }
    public int cap() { return cap; }

    private static int clamp(int cur, int cap) {
        return Math.max(0, Math.min(cur, cap));
    }
}
