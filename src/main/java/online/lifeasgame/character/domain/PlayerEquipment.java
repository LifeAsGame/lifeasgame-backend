package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@AggregateRoot
@Table(
        name = "player_equipment",
        uniqueConstraints = @UniqueConstraint(name = "uq_player_slot", columnNames = {"player_id", "slot_id"})
)
public class PlayerEquipment extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "item_inst_id", nullable = false)
    private Long itemInstanceId;

    @Column(name = "equipped_at", nullable = false)
    private Instant equippedAt = Instant.now();
}
