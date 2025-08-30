package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import online.lifeasgame.core.annotation.AggregateRoot;

@Entity
@AggregateRoot
@Table(
        name = "items",
        indexes = @Index(name = "idx_item_name", columnList = "name")
)
public class Item {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ItemCategory category;

    @Embedded
    private ItemType type;

    @Embedded
    private ItemName name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Rarity rarity = Rarity.COMMON;

    @Convert(converter = BaseAttrsConverter.class)
    @Column(name = "base_attrs", columnDefinition = "json")
    private BaseAttrs baseAttrs = BaseAttrs.empty();

    @Column(name = "stackable", nullable = false)
    private boolean stackable = false;

    @Column(name = "max_stack", nullable = false)
    private int maxStack = 1;

    @PrePersist
    @PreUpdate
    void validate() {
        if (!stackable && maxStack != 1)
            throw new IllegalStateException("non-stackable item must have maxStack=1");
        if (stackable && maxStack < 2)
            throw new IllegalStateException("stackable item must have maxStack >= 2");
    }
}
