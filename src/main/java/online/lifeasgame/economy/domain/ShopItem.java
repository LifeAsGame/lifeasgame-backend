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

    @Version
    private Long version;

    private ShopItem(Long itemId, Money price) {
        this.itemId = Guard.notNull(itemId, "itemId");
        this.price = Guard.notNull(price, "price");
    }

    public static ShopItem create(Long itemId, Money price) {
        return new ShopItem(itemId, price);
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
}
