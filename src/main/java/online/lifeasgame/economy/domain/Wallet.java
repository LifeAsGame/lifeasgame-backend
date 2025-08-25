package online.lifeasgame.economy.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

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

    public String placeHold(Money amt, String reason, Instant now, int ttlSeconds) {
        WalletBalance b = balance(amt.currency());
        Guard.check(b.available() >= amt.amount(), "insufficient funds");
        b.decrease(amt.amount());
        WalletHold h = WalletHold.open(this, amt.currency(), amt.amount(), reason, now, ttlSeconds);
        this.holds.add(h);
        return h.getHoldId();
    }

    public void commitHold(String holdId) {
        WalletHold h = requireHold(holdId);
        Guard.checkState(h.isOpen(), "hold not open");
        h.commit();
    }

    public void cancelHold(String holdId) {
        WalletHold h = requireHold(holdId);
        Guard.checkState(h.isOpen(), "hold not open");
        balance(h.getCurrency()).increase(h.getAmount());
        h.cancel();
    }

    public void expireHolds(Instant now) {
        for (WalletHold h : holds) {
            if (h.isOpen() && now.isAfter(h.getExpiresAt())) {
                balance(h.getCurrency()).increase(h.getAmount());
                h.expire();
            }
        }
    }

    private WalletHold requireHold(String holdId){
        return holds.stream().filter(h -> Objects.equals(h.getHoldId(), holdId)).findFirst().orElseThrow();
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
}
