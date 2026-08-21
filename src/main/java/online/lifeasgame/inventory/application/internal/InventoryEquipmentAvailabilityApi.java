package online.lifeasgame.inventory.application.internal;

public interface InventoryEquipmentAvailabilityApi {

    void replaceEquippedItem(
            Long ownerPlayerId,
            Long previousInventoryEntryId,
            Long inventoryEntryId
    );

    void releaseEquippedItem(Long ownerPlayerId, Long inventoryEntryId);
}
