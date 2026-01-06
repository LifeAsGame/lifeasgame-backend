package online.lifeasgame.inventory.application.query;

import java.util.List;

public interface InventoryQuery {

    List<InventoryEntryView> findInventoryEntries(Long playerId);
}
