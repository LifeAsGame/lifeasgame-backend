package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@AggregateRoot
@Table(name = "player_inventory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerInventory extends AbstractTime {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "capacity_slots", nullable = false)
    private int capacitySlots = 60;

    @Column(name = "weight_limit")
    private Integer weightLimit;

    private PlayerInventory(Long playerId) {
        this.playerId = playerId;
    }

    public static PlayerInventory of(Long playerId) {
        return new PlayerInventory(playerId);
    }
}
