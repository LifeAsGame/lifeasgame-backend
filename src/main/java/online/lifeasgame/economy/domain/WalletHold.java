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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@Table(name = "wallet_holds", indexes = {
        @Index(name = "idx_hold_wallet", columnList = "wallet_id"),
        @Index(name = "idx_hold_expires", columnList = "expires_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletHold extends AbstractTime {

    public enum Status {OPEN, COMMITTED, CANCELED, EXPIRED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Getter
    @Column(name = "hold_id", length = 36, unique = true, nullable = false)
    private String holdId;

    @Getter
    @Column(length = 10, nullable = false)
    private Currency currency;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "reason", length = 100)
    private String reason;

    @Getter
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    static WalletHold open(Wallet wallet, Currency currency, long amount, String reason, Instant now, int ttlSeconds) {
        WalletHold h = new WalletHold();
        h.wallet = wallet;
        h.currency = currency;
        h.amount = amount;
        h.reason = reason;
        h.holdId = UUID.randomUUID().toString();
        h.expiresAt = now.plusSeconds(ttlSeconds);
        return h;
    }

    void commit() {
        this.status = Status.COMMITTED;
    }

    void cancel() {
        this.status = Status.CANCELED;
    }

    void expire() {
        this.status = Status.EXPIRED;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public long getAmount() {
        return amount;
    }
}
