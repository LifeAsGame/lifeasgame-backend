package online.lifeasgame.economy.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "listings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_active_item", columnNames = {"item_inst_id", "active_flag"})
        },
        indexes = {
                @Index(name = "idx_status_price", columnList = "status,price"),
                @Index(name = "idx_seller", columnList = "seller_player_id"),
                @Index(name = "idx_item", columnList = "item_id"),
                @Index(name = "idx_reservation_exp", columnList = "reservation_expires_at")
        }
)
@AttributeOverrides({
        @AttributeOverride(name = "price.amount", column = @Column(name = "price")),
        @AttributeOverride(name = "price.currency", column = @Column(name = "currency", length = 10))
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Listing extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_player_id", nullable = false)
    private Long sellerPlayerId;

    @Column(name = "item_inst_id", nullable = false)
    private Long itemInstanceId;

    @Column(name = "item_id")
    private Long itemId;

    @Embedded
    private Money price = Money.of(0, Currency.GOLD);

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ListingStatus status = ListingStatus.OPEN;

    @Column(name = "active_flag")
    private Integer activeFlag = 1;

    @Embedded
    private ReservationToken reservationToken;

    @Column(name = "reserved_by")
    private Long reservedBy; // buyerId

    @Column(name = "reservation_expires_at")
    private Instant reservationExpiresAt;

    @Column(name = "reserved_hold_id", length = 36)
    private String reservedHoldId;

    @Version
    private Long version;

    private Listing(Long sellerPlayerId, Long itemInstanceId, Long itemId, Money price) {
        this.sellerPlayerId = Guard.notNull(sellerPlayerId, "sellerPlayerId");
        this.itemInstanceId = Guard.notNull(itemInstanceId, "itemInstanceId");
        this.itemId = itemId;
        changePrice(price);
    }

    public static Listing open(Long sellerPlayerId, Long itemInstanceId, Long itemId, Money price) {
        return new Listing(sellerPlayerId, itemInstanceId, itemId, price);
    }

    public void changePrice(Money newPrice) {
        Guard.notNull(newPrice, "price");
        Guard.checkState(status == ListingStatus.OPEN, "price change only in OPEN");
        this.price = newPrice;
    }

    public ReservationToken reserve(Long buyerId, String holdId, Instant now, int ttlSeconds) {
        Guard.checkState(status == ListingStatus.OPEN, "listing not open");
        Guard.notNull(buyerId, "buyerId");
        Guard.checkState(!sellerPlayerId.equals(buyerId), "seller cannot reserve own listing");
        Guard.notBlank(holdId, "holdId");
        Guard.notNull(now, "now");
        Guard.minValue(ttlSeconds, 1, "ttlSeconds");

        this.status = ListingStatus.RESERVED;
        this.reservedBy = Guard.notNull(buyerId, "buyerId");
        this.reservationToken = ReservationToken.newToken();
        this.reservationExpiresAt = now.plusSeconds(ttlSeconds);
        this.reservedHoldId = holdId;

        return this.reservationToken;
    }

    public void releaseReservation(String token, Instant now) {
        Guard.checkState(status == ListingStatus.RESERVED, "not reserved");
        Guard.check(this.reservationToken != null && this.reservationToken.value().equals(token), "invalid token");
        Guard.check(!now.isAfter(reservationExpiresAt), "already expired; use expire()");
        clearReservation();
        this.status = ListingStatus.OPEN;
    }

    public void expire(Instant now) {
        if (status == ListingStatus.RESERVED && reservationExpiresAt != null && now.isAfter(reservationExpiresAt)) {
            clearReservation();
            this.status = ListingStatus.EXPIRED;
            closeActive();
        }
    }

    public Trade sellTo(Long buyerPlayerId, String token) {
        Guard.notNull(buyerPlayerId, "buyerPlayerId");

        if (status == ListingStatus.OPEN) {
            Guard.checkState(!sellerPlayerId.equals(buyerPlayerId), "seller cannot buy own listing");
        } else {
            Guard.checkState(status == ListingStatus.RESERVED, "listing not available");
            Guard.checkState(this.reservedBy != null && this.reservedBy.equals(buyerPlayerId),
                    "reserved by other buyer");
            Guard.checkState(this.reservationToken != null && this.reservationToken.value().equals(token),
                    "invalid token");
            Guard.notBlank(this.reservedHoldId, "reservedHoldId");
        }
        this.status = ListingStatus.SOLD;
        closeActive();
        clearReservation();
        return Trade.of(this.id, buyerPlayerId, sellerPlayerId, itemInstanceId, price);
    }

    public void cancel(Long bySellerId) {
        Guard.checkState(status == ListingStatus.OPEN || status == ListingStatus.RESERVED, "already closed");
        Guard.checkState(sellerPlayerId.equals(bySellerId), "only seller can cancel");
        clearReservation();
        this.status = ListingStatus.CANCELED;
        closeActive();
    }

    private void clearReservation() {
        this.reservedBy = null;
        this.reservationToken = null;
        this.reservationExpiresAt = null;
        this.reservedHoldId = null;
    }

    private void closeActive() {
        this.activeFlag = null;
    }
}
