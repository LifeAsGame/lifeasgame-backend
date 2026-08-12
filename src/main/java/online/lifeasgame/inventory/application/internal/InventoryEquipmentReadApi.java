package online.lifeasgame.inventory.application.internal;

public interface InventoryEquipmentReadApi {

    OwnedEquipmentItem getOwnedItem(Long playerId, Long itemInstanceId);

    record OwnedEquipmentItem(
            Long itemInstanceId,
            Long itemId,
            String category,
            String type
    ) {
    }
}
