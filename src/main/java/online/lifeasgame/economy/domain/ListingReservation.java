package online.lifeasgame.economy.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Entity
@AggregateRoot
@Table(
        name = "listing_reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_listing_reservation_active",
                        columnNames = {"listing_id", "active_flag"}
                ),
                @UniqueConstraint(
                        name = "uq_listing_reservation_token",
                        columnNames = "reservation_token"
                )
        },
        indexes = {
                @Index(name = "idx_listing_reservation_expiry", columnList = "state,expires_at"),
                @Index(name = "idx_listing_reservation_buyer", columnList = "buyer_player_id,state")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListingReservation extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "buyer_player_id", nullable = false)
    private Long buyerPlayerId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "reservation_token", nullable = false, length = 36)
    )
    private ReservationToken token;

    @Column(name = "wallet_hold_id", nullable = false, length = 36)
    private String walletHoldId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private ListingReservationState state;

    @Column(name = "active_flag")
    private Integer activeFlag;

    @Version
    private Long version;

    private ListingReservation(
            Long listingId,
            Long buyerPlayerId,
            String walletHoldId,
            Instant now,
            int ttlSeconds
    ) {
        this.listingId = Guard.notNull(listingId, "listingId");
        this.buyerPlayerId = Guard.notNull(buyerPlayerId, "buyerPlayerId");
        this.walletHoldId = Guard.maxLength(
                Guard.notBlank(walletHoldId, "walletHoldId"),
                36,
                "walletHoldId"
        );
        this.token = ReservationToken.newToken();
        this.expiresAt = Guard.notNull(now, "now").plusSeconds(
                Guard.minValue(ttlSeconds, 1, "ttlSeconds")
        );
        this.state = ListingReservationState.ACTIVE;
        this.activeFlag = 1;
    }

    public static ListingReservation active(
            Long listingId,
            Long buyerPlayerId,
            String walletHoldId,
            Instant now,
            int ttlSeconds
    ) {
        return new ListingReservation(listingId, buyerPlayerId, walletHoldId, now, ttlSeconds);
    }

    public boolean isExpiredAt(Instant now) {
        return state == ListingReservationState.ACTIVE && now.isAfter(expiresAt);
    }

    public void expire(Instant now) {
        if (!isExpiredAt(now)) {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }
        close(ListingReservationState.EXPIRED);
    }

    public void consume(Long buyerId, String reservationToken, Instant now) {
        if (isExpiredAt(now)) {
            throw new DomainException(EconomyError.LISTING_RESERVATION_EXPIRED);
        }
        if (state != ListingReservationState.ACTIVE) {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }
        if (!buyerPlayerId.equals(buyerId)) {
            throw new DomainException(EconomyError.LISTING_RESERVED_OTHER);
        }
        if (!token.value().equals(reservationToken)) {
            throw new DomainException(EconomyError.INVALID_RESERVATION_TOKEN);
        }
        close(ListingReservationState.CONSUMED);
    }

    private void close(ListingReservationState terminalState) {
        this.state = terminalState;
        this.activeFlag = null;
    }

    public Long getId() {
        return id;
    }

    public Long getListingId() {
        return listingId;
    }

    public Long getBuyerPlayerId() {
        return buyerPlayerId;
    }

    public String getReservationToken() {
        return token.value();
    }

    public String getWalletHoldId() {
        return walletHoldId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ListingReservationState getState() {
        return state;
    }
}
