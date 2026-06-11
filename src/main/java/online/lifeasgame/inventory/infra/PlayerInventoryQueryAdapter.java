package online.lifeasgame.inventory.infra;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.query.InventoryEntryView;
import online.lifeasgame.inventory.application.query.InventoryQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerInventoryQueryAdapter implements InventoryQuery {

    private final EntityManager em;

    @Override
    public List<InventoryEntryView> findInventoryEntries(Long playerId) {
        return em.createQuery("""
            SELECT new online.lifeasgame.inventory.application.query.InventoryEntryView(
                e.id,
                e.slotIndex.value,
                i.id,
                i.name.value,
                i.category,
                i.type,
                i.rarity,
                i.stackable,
                i.maxStack,
                e.quantity.value,
                e.bound,
                e.durability.value,
                e.instAttrs
            )
            FROM InventoryEntry e
            JOIN e.inventory inv
            JOIN Item i ON i.id = e.itemId
            WHERE inv.playerId = :playerId
            ORDER BY e.slotIndex.value
        """, InventoryEntryView.class)
                .setParameter("playerId", playerId)
                .getResultList();
    }

    @Override
    public Optional<InventoryEntryView> findInventoryEntryByInstanceId(Long playerId, Long itemInstanceId) {
        return em.createQuery("""
            SELECT new online.lifeasgame.inventory.application.query.InventoryEntryView(
                e.id,
                e.slotIndex.value,
                i.id,
                i.name.value,
                i.category,
                i.type,
                i.rarity,
                i.stackable,
                i.maxStack,
                e.quantity.value,
                e.bound,
                e.durability.value,
                e.instAttrs
            )
            FROM InventoryEntry e
            JOIN e.inventory inv
            JOIN Item i ON i.id = e.itemId
            WHERE inv.playerId = :playerId AND e.id = :itemInstanceId
        """, InventoryEntryView.class)
                .setParameter("playerId", playerId)
                .setParameter("itemInstanceId", itemInstanceId)
                .getResultStream()
                .findFirst();
    }
}
