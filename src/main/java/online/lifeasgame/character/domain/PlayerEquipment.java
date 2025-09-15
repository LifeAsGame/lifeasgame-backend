package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Entity
@AggregateRoot
@Table(
        name = "player_equipment",
        uniqueConstraints = @UniqueConstraint(name = "uq_player_slot", columnNames = {"player_id", "slot_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerEquipment extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playerId", nullable = false)
    private Long playerId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "item_inst_id", nullable = false)
    private Long itemInstanceId;

    @Column(name = "equipped_at", nullable = false)
    private Instant equippedAt;

    private PlayerEquipment(Long playerId, Long slotId, Long itemInstanceId, Instant equippedAt) {
        this.playerId = playerId;
        this.slotId = slotId;
        this.itemInstanceId = itemInstanceId;
        this.equippedAt = equippedAt == null ? Instant.now() : equippedAt;
    }

    public static PlayerEquipment equip(Long playerId, Long slotId, Long itemInstanceId) {
        return new PlayerEquipment(playerId, slotId, itemInstanceId, Instant.now());
    }
}
