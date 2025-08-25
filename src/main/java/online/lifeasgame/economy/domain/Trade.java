package online.lifeasgame.economy.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;


@Entity
@AggregateRoot
@Table(
        name = "trades",
        indexes = {
                @Index(name = "idx_buyer_time", columnList = "buyer_player_id,created_at"),
                @Index(name = "idx_seller_time", columnList = "seller_player_id,created_at")
        }
)
@AttributeOverrides({
        @AttributeOverride(name = "price.amount", column = @Column(name = "price")),
        @AttributeOverride(name = "price.currency", column = @Column(name = "currency", length = 10)),
        @AttributeOverride(name = "fee.amount", column = @Column(name = "fee")),
        @AttributeOverride(name = "fee.currency", column = @Column(name = "fee_currency", length = 10)),
        @AttributeOverride(name = "sellerProceeds.amount", column = @Column(name = "seller_proceeds")),
        @AttributeOverride(name = "sellerProceeds.currency", column = @Column(name = "seller_proceeds_currency", length = 10))
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "buyer_player_id", nullable = false)
    private Long buyerPlayerId;

    @Column(name = "seller_player_id", nullable = false)
    private Long sellerPlayerId;

    @Column(name = "item_inst_id", nullable = false)
    private Long itemInstanceId;

    @Embedded
    private Money price = Money.of(0, Currency.GOLD);

    @Embedded
    private Money fee = Money.of(0, Currency.GOLD);

    @Embedded
    private Money sellerProceeds = Money.of(0, Currency.GOLD);

    @Column(name = "fee_bps", nullable = false)
    private Integer feeBps = 100;

    private Trade(Long listingId, Long buyerId, Long sellerId, Long itemInstId, Money price, int feeBps) {
        this.listingId = Guard.notNull(listingId, "listingId");
        this.buyerPlayerId = Guard.notNull(buyerId, "buyerPlayerId");
        this.sellerPlayerId = Guard.notNull(sellerId, "sellerPlayerId");
        this.itemInstanceId = Guard.notNull(itemInstId, "itemInstanceId");
        this.price = Guard.notNull(price, "price");
        this.feeBps = feeBps;
        this.fee = price.percent(feeBps);
        this.sellerProceeds = price.subtract(this.fee);
    }


    static Trade of(Long listingId, Long buyerId, Long sellerId, Long itemInstId, Money price) {
        return new Trade(listingId, buyerId, sellerId, itemInstId, price, 100);
    }
}
