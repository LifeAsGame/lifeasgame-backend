package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "inventory_reward_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inventory_reward_delivery_line",
                columnNames = "reward_line_id"
        ),
        indexes = @Index(
                name = "idx_inventory_reward_delivery_player",
                columnList = "player_id, delivered_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryRewardDelivery extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reward_line_id", nullable = false)
    private Long rewardLineId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "item_code", length = ItemCode.MAX_LENGTH, nullable = false)
    private String itemCode;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;

    private InventoryRewardDelivery(
            Long rewardLineId,
            Long playerId,
            ItemCode itemCode,
            Long itemId,
            long quantity,
            Instant deliveredAt
    ) {
        validate(rewardLineId, playerId, itemCode, itemId, quantity, deliveredAt);
        this.rewardLineId = rewardLineId;
        this.playerId = playerId;
        this.itemCode = itemCode.value();
        this.itemId = itemId;
        this.quantity = quantity;
        this.deliveredAt = deliveredAt;
    }

    public static InventoryRewardDelivery create(
            Long rewardLineId,
            Long playerId,
            ItemCode itemCode,
            Long itemId,
            long quantity,
            Instant deliveredAt
    ) {
        return new InventoryRewardDelivery(
                rewardLineId,
                playerId,
                itemCode,
                itemId,
                quantity,
                deliveredAt
        );
    }

    public void assertMatches(
            Long rewardLineId,
            Long playerId,
            ItemCode itemCode,
            long quantity
    ) {
        if (!this.rewardLineId.equals(rewardLineId)
                || !this.playerId.equals(playerId)
                || !this.itemCode.equals(itemCode.value())
                || this.quantity != quantity) {
            throw new DomainException(InventoryError.REWARD_DELIVERY_CONFLICT);
        }
    }

    private static void validate(
            Long rewardLineId,
            Long playerId,
            ItemCode itemCode,
            Long itemId,
            long quantity,
            Instant deliveredAt
    ) {
        if (rewardLineId == null || rewardLineId <= 0) {
            throw new DomainException(InventoryError.REWARD_LINE_ID_INVALID);
        }
        if (playerId == null || playerId <= 0) {
            throw new DomainException(InventoryError.PLAYER_ID_INVALID);
        }
        if (itemCode == null) {
            throw new DomainException(InventoryError.REWARD_ITEM_CODE_INVALID);
        }
        if (itemId == null || itemId <= 0) {
            throw new DomainException(InventoryError.ITEM_NOT_FOUND);
        }
        if (quantity <= 0) {
            throw new DomainException(InventoryError.REWARD_QUANTITY_INVALID);
        }
        if (deliveredAt == null) {
            throw new DomainException(InventoryError.REWARD_DELIVERY_CONFLICT);
        }
    }
}
