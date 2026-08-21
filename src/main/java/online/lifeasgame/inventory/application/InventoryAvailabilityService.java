package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentAvailabilityApi;
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi;
import online.lifeasgame.inventory.domain.InventoryEntry;
import online.lifeasgame.inventory.domain.PlayerInventory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryAvailabilityService implements
        InventoryMarketAvailabilityApi,
        InventoryEquipmentAvailabilityApi {

    private final InventoryReader inventoryReader;

    @Override
    @Transactional
    public EntrySnapshot listWholeEntry(
            Long ownerPlayerId,
            Long inventoryEntryId
    ) {
        return transition(
                ownerPlayerId,
                inventoryEntryId,
                InventoryEntry::listForMarket
        );
    }

    @Override
    @Transactional
    public EntrySnapshot reserveForTrade(
            Long ownerPlayerId,
            Long inventoryEntryId
    ) {
        return transition(
                ownerPlayerId,
                inventoryEntryId,
                InventoryEntry::reserveForTrade
        );
    }

    @Override
    @Transactional
    public EntrySnapshot releaseTradeReservation(
            Long ownerPlayerId,
            Long inventoryEntryId
    ) {
        return transition(
                ownerPlayerId,
                inventoryEntryId,
                InventoryEntry::releaseTradeReservation
        );
    }

    @Override
    @Transactional
    public EntrySnapshot releaseListing(
            Long ownerPlayerId,
            Long inventoryEntryId
    ) {
        return transition(
                ownerPlayerId,
                inventoryEntryId,
                InventoryEntry::releaseListing
        );
    }

    @Override
    @Transactional
    public EntrySnapshot beginTransfer(
            Long ownerPlayerId,
            Long inventoryEntryId
    ) {
        return transition(
                ownerPlayerId,
                inventoryEntryId,
                InventoryEntry::beginTransfer
        );
    }

    @Override
    @Transactional(readOnly = true)
    public EntrySnapshot getSnapshot(
            Long ownerPlayerId,
            Long inventoryEntryId
    ) {
        PlayerInventory inventory = inventoryReader
                .getByPlayerIdOrThrow(ownerPlayerId);
        return snapshot(
                ownerPlayerId,
                inventory.getEntryByIdOrThrow(inventoryEntryId)
        );
    }

    @Override
    @Transactional
    public void replaceEquippedItem(
            Long ownerPlayerId,
            Long previousInventoryEntryId,
            Long inventoryEntryId
    ) {
        inventoryReader.getByPlayerIdForUpdateOrThrow(ownerPlayerId)
                .replaceEquippedEntry(
                        previousInventoryEntryId,
                        inventoryEntryId
                );
    }

    @Override
    @Transactional
    public void releaseEquippedItem(
            Long ownerPlayerId,
            Long inventoryEntryId
    ) {
        inventoryReader.getByPlayerIdForUpdateOrThrow(ownerPlayerId)
                .releaseEquippedEntry(inventoryEntryId);
    }

    private EntrySnapshot transition(
            Long ownerPlayerId,
            Long inventoryEntryId,
            java.util.function.Consumer<InventoryEntry> operation
    ) {
        PlayerInventory inventory = inventoryReader
                .getByPlayerIdForUpdateOrThrow(ownerPlayerId);
        InventoryEntry entry = inventory.getEntryByIdOrThrow(
                inventoryEntryId
        );
        operation.accept(entry);
        return snapshot(ownerPlayerId, entry);
    }

    private EntrySnapshot snapshot(
            Long ownerPlayerId,
            InventoryEntry entry
    ) {
        return new EntrySnapshot(
                entry.getId(),
                ownerPlayerId,
                entry.getItemId(),
                entry.getQuantity().value(),
                entry.getAvailability().name()
        );
    }
}
