package online.lifeasgame.inventory.application.internal;

public interface InventoryMarketAvailabilityApi {

    EntrySnapshot listWholeEntry(Long ownerPlayerId, Long inventoryEntryId);

    EntrySnapshot reserveForTrade(Long ownerPlayerId, Long inventoryEntryId);

    EntrySnapshot releaseTradeReservation(Long ownerPlayerId, Long inventoryEntryId);

    EntrySnapshot releaseListing(Long ownerPlayerId, Long inventoryEntryId);

    EntrySnapshot beginTransfer(Long ownerPlayerId, Long inventoryEntryId);

    EntrySnapshot getSnapshot(Long ownerPlayerId, Long inventoryEntryId);

    record EntrySnapshot(
            Long inventoryEntryId,
            Long ownerPlayerId,
            Long itemId,
            int quantity,
            String availability
    ) {
    }
}
