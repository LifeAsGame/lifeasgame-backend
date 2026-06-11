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
        name = "shop_purchases",
        uniqueConstraints = @UniqueConstraint(name = "uq_shop_res_token", columnNames = "reservation_token"),
        indexes = {
                @Index(name = "idx_shop_item", columnList = "shop_item_id"),
                @Index(name = "idx_shop_player", columnList = "player_id"),
                @Index(name = "idx_shop_status", columnList = "status")
        }
)
@AttributeOverrides(
        {
                @AttributeOverride(name = "totalPrice.amount", column = @Column(name = "total_price")),
                @AttributeOverride(name = "totalPrice.currency", column = @Column(name = "currency", length = 10))
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopPurchase extends AbstractTime {

    public enum Status {REQUESTED, RESERVED, COMPLETED, CANCELED, EXPIRED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_item_id", nullable = false)
    private Long shopItemId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private Status status = Status.REQUESTED;

    @Embedded
    private Money totalPrice = Money.of(0, Currency.GOLD);

    @Column(name = "reservation_token", length = 36)
    private String reservationToken;

    @Column(name = "reservation_expires_at")
    private Instant reservationExpiresAt;

    @Column(name = "wallet_hold_id", length = 36)
    private String walletHoldId;

    private ShopPurchase(
            Long shopItemId,
            Long playerId,
            Integer quantity,
            Money totalPrice
    ) {
        this.shopItemId = Guard.notNull(shopItemId, "shopItemId");
        this.playerId = Guard.notNull(playerId, "playerId");
        this.quantity = Guard.notNull(quantity, "quantity");
        this.totalPrice = Guard.notNull(totalPrice, "totalPrice");
    }

    public static ShopPurchase request(
            Long shopItemId,
            Long playerId,
            int quantity,
            Money unitPrice
    ) {
        Guard.minValue(quantity, 1, "quantity");
        return new ShopPurchase(
                shopItemId,
                playerId,
                quantity,
                unitPrice.multiply(quantity)
        );
    }

    public void reserve(ReservationToken token, Instant expiresAt, String walletHoldId) {
        Guard.checkState(status == Status.REQUESTED, "only requested can be reserved");
        this.status = Status.RESERVED;
        this.reservationToken = token.value();
        this.reservationExpiresAt = expiresAt;
        this.walletHoldId = walletHoldId;
    }

    public void completeFromReservation(String token) {
        Guard.checkState(status == Status.RESERVED, "not reserved");
        Guard.check(Guard.notBlank(token, "token").equals(reservationToken), "invalid reservation token");
        this.status = Status.COMPLETED;
        clearReservation();
    }

    public void completeImmediately() {
        Guard.checkState(status == Status.REQUESTED, "already processed");
        this.status = Status.COMPLETED;
    }

    public void cancel() {
        if (status == Status.COMPLETED) {
            return;
        }
        this.status = Status.CANCELED;
        clearReservation();
    }

    public void expire(Instant now) {
        if (status != Status.RESERVED || reservationExpiresAt == null) {
            return;
        }
        if (now.isAfter(reservationExpiresAt)) {
            this.status = Status.EXPIRED;
            clearReservation();
        }
    }

    private void clearReservation() {
        this.reservationToken = null;
        this.reservationExpiresAt = null;
        this.walletHoldId = null;
    }

    public Long getId() {
        return id;
    }

    public Long getShopItemId() {
        return shopItemId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Status getStatus() {
        return status;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public String getReservationToken() {
        return reservationToken;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public String getWalletHoldId() {
        return walletHoldId;
    }
}
