package online.lifeasgame.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@Table(
        name = "wallet_balances",
        indexes = @Index(name = "idx_balance_wallet", columnList = "wallet_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_wallet_balance_wallet_currency", columnNames = {"wallet_id", "currency"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletBalance extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Currency currency;

    @Column(name = "amount", nullable = false)
    private Long amount;

    public WalletBalance(Wallet wallet, Currency currency, Long amount) {
        this.wallet = wallet;
        this.currency = currency;
        this.amount = amount;
    }

    static WalletBalance of(Wallet wallet, Currency currency, long amount) {
        return new WalletBalance(wallet, currency, amount);
    }

    void increase(long v) {
        Guard.minValue(v, 0, "increase amount");
        this.amount += v;
    }

    void decrease(long v) {
        Guard.minValue(v, 0, "decrease amount");
        Guard.check(this.amount >= v, "insufficient balance");
        this.amount -= v;
    }

    public Currency getCurrency() {
        return currency;
    }

    public long available() {
        return amount;
    }
}
