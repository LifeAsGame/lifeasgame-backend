package online.lifeasgame.economy.domain;

import jakarta.persistence.*;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@AggregateRoot
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(name = "uq_wallet_owner", columnNames = "owner_id"))
public class Wallet extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId; // Player/User id

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "wallet",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<WalletBalance> balances = new ArrayList<>();

    @OneToMany(
            mappedBy = "wallet",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<WalletHold> holds = new ArrayList<>();

    public static Wallet open(Long ownerId) {
        Wallet w = new Wallet();
        w.ownerId = Guard.notNull(ownerId, "ownerId");
        return w;
    }

    public void deposit(Money amt) {
        balance(amt.currency()).increase(amt.amount());
    }

    public void withdraw(Money amt) {
        balance(amt.currency()).decrease(amt.amount());
    }

    public String placeHold(Money amount, String reason, Instant now, int ttlSeconds) {
        WalletBalance walletBalance = balance(amount.currency());
        Guard.check(walletBalance.available() >= amount.amount(), "insufficient funds");
        walletBalance.decrease(amount.amount());

        WalletHold h = WalletHold.open(
                this,
                amount.currency(),
                amount.amount(),
                reason,
                now,
                ttlSeconds
        );

        this.holds.add(h);
        return h.getHoldId();
    }

    public void commitHold(String holdId) {
        WalletHold h = requireHold(holdId);
        Guard.checkState(h.isOpen(), "hold not open");
        h.commit();
    }

    public void cancelHold(String holdId) {
        WalletHold walletHold = requireHold(holdId);
        Guard.checkState(walletHold.isOpen(), "hold not open");
        balance(walletHold.getCurrency()).increase(walletHold.getAmount());
        walletHold.cancel();
    }

    public void expireHolds(Instant now) {
        for (WalletHold walletHold : holds) {
            if (walletHold.isOpen() && now.isAfter(walletHold.getExpiresAt())) {
                balance(walletHold.getCurrency()).increase(walletHold.getAmount());
                walletHold.expire();
            }
        }
    }

    private WalletHold requireHold(String holdId){
        return holds.stream()
                .filter(h -> Objects.equals(h.getHoldId(), holdId))
                .findFirst()
                .orElseThrow();
    }

    private WalletBalance balance(Currency currency){
        Guard.notNull(currency, "currency");
        Optional<WalletBalance> o = balances.stream()
                .filter(b -> b.getCurrency() == currency)
                .findFirst();

        if (o.isPresent()) {
            return o.get();
        }

        WalletBalance nb = WalletBalance.of(this, currency, 0L);
        balances.add(nb);
        return nb;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public WalletBalance getBalance(Currency currency) {
        return balance(currency);
    }

    public WalletBalance getBalance() {
        return balance(Currency.GOLD);
    }
}
