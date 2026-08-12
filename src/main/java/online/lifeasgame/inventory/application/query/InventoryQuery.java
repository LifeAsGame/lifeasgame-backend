package online.lifeasgame.inventory.application.query;

import java.util.List;
import java.util.Optional;

public interface InventoryQuery {

    List<InventoryEntryView> findInventoryEntries(Long playerId);

    Optional<InventoryEntryView> findInventoryEntryByInstanceId(Long playerId, Long itemInstanceId);

    Optional<OwnedEquipmentItemView> findOwnedEquipmentItem(Long playerId, Long itemInstanceId);
}
