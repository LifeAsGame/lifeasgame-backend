package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.query.InventoryEntryView;
import online.lifeasgame.inventory.application.query.InventoryQuery;
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
}
