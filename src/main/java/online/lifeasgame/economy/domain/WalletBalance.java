package online.lifeasgame.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@Table(name = "wallet_balances", indexes = @Index(name = "idx_balance_wallet", columnList = "wallet_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletBalance extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

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
        this.amount += v;
    }

    void decrease(long v) {
        Guard.check(this.amount < v, "insufficient balance");
        this.amount -= v;
    }

    public Currency getCurrency() {
        return currency;
    }

    public long available() {
        return amount;
    }
}
