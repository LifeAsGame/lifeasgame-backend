package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.query.InventoryEntryView;
import online.lifeasgame.inventory.application.query.InventoryQuery;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class InventoryQueryReader {

    private final InventoryQuery inventoryQuery;

    public List<InventoryEntryView> list(Long playerId) {
        return inventoryQuery.findInventoryEntries(playerId);
    }

    public InventoryEntryView getEntry(Long playerId, Long itemInstanceId) {
        return inventoryQuery.findInventoryEntryByInstanceId(playerId, itemInstanceId)
                .orElseThrow(() -> new DomainException(InventoryError.INVENTORY_ENTRY_NOT_FOUND));
    }
}
