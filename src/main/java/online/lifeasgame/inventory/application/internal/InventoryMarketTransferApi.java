package online.lifeasgame.inventory.application.internal;

public interface InventoryMarketTransferApi {

    TransferResult transferWholeEntry(
            Long sellerPlayerId,
            Long buyerPlayerId,
            Long inventoryEntryId,
            Long expectedItemId,
            int expectedQuantity
    );

    record TransferResult(
            Long sourceInventoryEntryId,
            Long itemId,
            int quantity
    ) {
    }
}
