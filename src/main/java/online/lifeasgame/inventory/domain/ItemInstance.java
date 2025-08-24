package online.lifeasgame.inventory.domain;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import online.lifeasgame.shared.annotation.AggregateRoot;

@AggregateRoot
@Entity
@Table(name = "item_instances")
public class ItemInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "owner_player_id", nullable = false)
    private Long ownerPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ItemLocation location = ItemLocation.INVENTORY;

    @Column(name = "slot_index")
    private Integer slotIndex;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "quantity", nullable = false))
    private Quantity quantity = Quantity.of(1);

    @Column
    private Integer durability;

    @Column(nullable = false)
    private boolean bound = false;

    @Convert(converter = InstanceAttrsConverter.class)
    @Column(name = "inst_attrs", columnDefinition = "json")
    private InstanceAttrs instAttrs = InstanceAttrs.empty();

    @Version
    private Long version;
}
