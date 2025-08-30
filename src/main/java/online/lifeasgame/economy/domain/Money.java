package online.lifeasgame.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10, nullable = false)
    private Currency currency;

    private Money(long amount, Currency currency) {
        Guard.minValue(amount, 0, "amount");
        this.amount = amount;
        this.currency = Guard.notNull(currency, "currency");
    }

    public static Money of(long amount, Currency currency) {
        return new Money(amount, currency);
    }

    public long amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount + other.amount, currency);
    }

    public Money subtract(Money other) {
        ensureSameCurrency(other);
        long next = this.amount - other.amount;
        Guard.minValue(next, 0, "result amount");
        return new Money(next, currency);
    }

    public Money percent(int bps) {
        Guard.inRange(bps, 0, 100_000, "bps");
        long v = (this.amount * bps) / 10_000L;
        return new Money(v, currency);
    }

    private void ensureSameCurrency(Money other) {
        Guard.check(Objects.equals(currency, other.currency), "currency mismatch");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money m)) {
            return false;
        }
        return amount.equals(m.amount) && currency.equals(m.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
