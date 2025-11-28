package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.model.InventorySpec;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryReader inventoryReader;
    private final InventoryWriter inventoryWriter;
    private final ItemReader itemReader;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public InventoryResult.Slots add(Long playerId, InventoryCommand.Add command) {
        Item item = itemReader.getItem(command.itemId());
        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);

        List<SlotIndex> slotIndexes = inventoryWriter.add(
                playerInventory,
                ItemCarryPolicy.from(item),
                InventorySpec.Add.from(command)
        );

        domainEventPublisher.publishAll(playerInventory.pullEvents());

        return InventoryResult.Slots.fromList(slotIndexes);
    }

    @Transactional(readOnly = true)
    public InventoryResult.Entries list(Long playerId) {
        List<InventoryEntry> entries = inventoryReader.getPlayerInventory(playerId).getEntries();
        return InventoryResult.Entries.fromList(entries);
    }

    @Transactional
    public void remove(Long playerId, InventoryCommand.Remove command) {
        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);
        inventoryWriter.remove(playerInventory, SlotIndex.of(command.slotIndex()), command.quantity());
    }

    @Transactional
    public void move(Long playerId, InventoryCommand.Move command) {
        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);
        inventoryWriter.move(playerInventory, SlotIndex.of(command.from()), SlotIndex.of(command.to()));
    }

    @Transactional
    public void merge(Long playerId, InventoryCommand.Merge command) {
        SlotIndex from = SlotIndex.of(command.from());
        SlotIndex to = SlotIndex.of(command.to());

        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);
        InventoryEntry fromEntry = playerInventory.getEntry(from);

        Item item = itemReader.getItem(fromEntry.getItemId());
        ItemCarryPolicy fromItemCarryPolicy = ItemCarryPolicy.from(item);

        inventoryWriter.merge(
                playerInventory,
                fromItemCarryPolicy,
                from,
                to
        );
    }

    @Transactional
    public InventoryResult.Slot split(Long playerId, InventoryCommand.Split command) {
        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);
        InventoryEntry inventoryEntry = playerInventory.getEntry(SlotIndex.of(command.from()));

        Item item = itemReader.getItem(inventoryEntry.getItemId());
        ItemCarryPolicy itemCarryPolicy = ItemCarryPolicy.from(item);

        SlotIndex slotIndex = inventoryWriter.split(
                playerInventory,
                itemCarryPolicy,
                SlotIndex.of(command.from()),
                SlotIndex.ofNullable(command.to()),
                command.quantity()
        );

        return InventoryResult.Slot.of(slotIndex.value());
    }
}
