package online.lifeasgame.character.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Getter
@Entity
@AggregateRoot
@Table(
        name = "player_equipment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_player_slot",
                        columnNames = {"player_id", "slot_id"}
                ),
                @UniqueConstraint(
                        name = "uq_player_equipment_item",
                        columnNames = {"player_id", "item_inst_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerEquipment extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "item_inst_id")
    private Long itemInstanceId;

    @Column(name = "equipped_at")
    private Instant equippedAt;

    private PlayerEquipment(Long playerId, Long slotId, Long itemInstanceId) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.slotId = Guard.notNull(slotId, "slotId");
        this.itemInstanceId = itemInstanceId;
        this.equippedAt = Instant.now();
    }

    public static PlayerEquipment create(Long playerId, Long slotId, Long itemInstanceId) {
        return new PlayerEquipment(playerId, slotId, itemInstanceId);
    }

    public void equip(Long itemInstanceId) {
        this.itemInstanceId = itemInstanceId;
        this.equippedAt = Instant.now();
    }

    public void unEquip() {
        this.itemInstanceId = null;
        this.equippedAt = null;
    }
}
