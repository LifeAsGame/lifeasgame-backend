package online.lifeasgame.economy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

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

    @Column(name = "sale_quantity")
    private Integer saleQuantity;

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

    private Listing(
            Long sellerPlayerId,
            Long itemInstanceId,
            Long itemId,
            int saleQuantity,
            Money price
    ) {
        this.sellerPlayerId = Guard.notNull(sellerPlayerId, "sellerPlayerId");
        this.itemInstanceId = Guard.notNull(itemInstanceId, "itemInstanceId");
        this.itemId = Guard.notNull(itemId, "itemId");
        this.saleQuantity = Guard.minValue(saleQuantity, 1, "saleQuantity");
        changePrice(price);
    }

    public static Listing open(
            Long sellerPlayerId,
            Long itemInstanceId,
            Long itemId,
            int saleQuantity,
            Money price
    ) {
        return new Listing(sellerPlayerId, itemInstanceId, itemId, saleQuantity, price);
    }

    public void changePrice(Money newPrice) {
        Guard.notNull(newPrice, "price");
        Guard.checkState(status == ListingStatus.OPEN, "price change only in OPEN");
        Guard.minValue(newPrice.amount(), 1, "price");
        this.price = newPrice;
    }

    public Trade sellTo(Long buyerPlayerId) {
        Guard.notNull(buyerPlayerId, "buyerPlayerId");
        Guard.checkState(status == ListingStatus.OPEN, "listing not available");
        Guard.checkState(!sellerPlayerId.equals(buyerPlayerId), "seller cannot buy own listing");
        this.status = ListingStatus.SOLD;
        closeActive();
        clearReservation();
        return Trade.of(
                this.id,
                buyerPlayerId,
                sellerPlayerId,
                itemInstanceId,
                Guard.notNull(itemId, "itemId"),
                Guard.notNull(saleQuantity, "saleQuantity"),
                price
        );
    }

    public void cancel(Long bySellerId) {
        Guard.checkState(status == ListingStatus.OPEN, "listing not open");
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

    public Money getPrice() {
        return price;
    }

    public Long getSellerPlayerId() {
        return sellerPlayerId;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public Long getReservedBy() {
        return reservedBy;
    }

    public ReservationToken getReservationToken() {
        return reservationToken;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public String getReservedHoldId() {
        return reservedHoldId;
    }

    public Long getItemInstanceId() {
        return itemInstanceId;
    }

    public Long getItemId() {
        return itemId;
    }

    public Integer getSaleQuantity() {
        return saleQuantity;
    }

    public Long getId() {
        return id;
    }
}
