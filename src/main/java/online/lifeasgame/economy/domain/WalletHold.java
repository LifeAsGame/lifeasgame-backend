package online.lifeasgame.economy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_holds", indexes = {
        @Index(name = "idx_hold_wallet", columnList = "wallet_id"),
        @Index(name = "idx_hold_expires", columnList = "expires_at")
}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletHold extends AbstractTime {

    public enum Status {
        OPEN, COMMITTED, CANCELED, EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "hold_id", length = 36, unique = true, nullable = false)
    private String holdId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Currency currency;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "reason", length = 100)
    private String reason;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public WalletHold(
            Wallet wallet,
            String holdId,
            Currency currency,
            Long amount,
            String reason,
            Instant expiresAt
    ) {
        this.wallet = wallet;
        this.holdId = holdId;
        this.currency = currency;
        this.amount = amount;
        this.reason = reason;
        this.expiresAt = expiresAt;
    }

    static WalletHold open(
            Wallet wallet,
            Currency currency,
            long amount,
            String reason,
            Instant now,
            int ttlSeconds
    ) {
        Guard.minValue(amount, 1, "amount");
        Guard.notNull(now, "now");
        Guard.minValue(ttlSeconds, 1, "ttlSeconds");
        return new WalletHold(
                wallet,
                UUID.randomUUID().toString(),
                currency,
                amount,
                reason,
                now.plusSeconds(ttlSeconds)
        );
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

    public String getHoldId() {
        return holdId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
