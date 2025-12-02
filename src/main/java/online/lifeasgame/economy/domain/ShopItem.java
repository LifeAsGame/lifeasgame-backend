package online.lifeasgame.economy.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "shop_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_shop_item", columnNames = {"item_id", "currency"})
)
@AttributeOverrides({
        @AttributeOverride(name = "price.amount", column = @Column(name = "price")),
        @AttributeOverride(name = "price.currency", column = @Column(name = "currency", length = 10))
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopItem extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Embedded
    private Money price = Money.of(0, Currency.GOLD);

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "global_stock_limit")
    private Integer globalStockLimit;

    @Column(name = "per_player_limit")
    private Integer perPlayerLimit;

    @Column(name = "reservation_ttl_sec")
    private Integer reservationTtlSec = 0;

    @Version
    private Long version;

    private ShopItem(Long itemId, Money price, Integer globalStockLimit, Integer perPlayerLimit, Integer reservationTtlSec) {
        this.itemId = Guard.notNull(itemId, "itemId");
        this.price = Guard.notNull(price, "price");
        this.globalStockLimit = globalStockLimit;
        this.perPlayerLimit = perPlayerLimit;
        this.reservationTtlSec = reservationTtlSec;
    }

    public static ShopItem create(Long itemId, Money price) {
        return new ShopItem(itemId, price, null, null, 0);
    }

    public static ShopItem createLimited(Long itemId, Money price, Integer globalStockLimit, Integer perPlayerLimit,
                                         Integer reservationTtlSec) {
        return new ShopItem(itemId, price, globalStockLimit, perPlayerLimit, reservationTtlSec);
    }

    public void changePrice(Money newPrice) {
        Guard.notNull(newPrice, "new price");
        Guard.check(newPrice.currency() == this.price.currency(), "currency cannot change");
        this.price = newPrice;
    }

    public void enable() {
        this.available = true;
    }

    public void disable() {
        this.available = false;
    }

    public void changeLimits(Integer globalStockLimit, Integer perPlayerLimit, Integer reservationTtlSec) {
        if (globalStockLimit != null) {
            Guard.minValue(globalStockLimit, 0, "globalStockLimit");
        }
        if (perPlayerLimit != null) {
            Guard.minValue(perPlayerLimit, 0, "perPlayerLimit");
        }
        if (reservationTtlSec != null) {
            Guard.minValue(reservationTtlSec, 0, "reservationTtlSec");
        }

        this.globalStockLimit = globalStockLimit;
        this.perPlayerLimit = perPlayerLimit;
        this.reservationTtlSec = reservationTtlSec;
    }

    public boolean isAvailable() {
        return available;
    }

    public Money getPrice() {
        return price;
    }

    public Integer getGlobalStockLimit() {
        return globalStockLimit;
    }

    public Integer getPerPlayerLimit() {
        return perPlayerLimit;
    }

    public int getReservationTtlSec() {
        return reservationTtlSec == null ? 0 : reservationTtlSec;
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getId() {
        return id;
    }
}
