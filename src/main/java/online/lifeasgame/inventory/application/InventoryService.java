package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
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

    @Transactional
    public InventoryResult.Slots add(Long playerId, InventoryCommand.Add command) {
        Item item = itemReader.getItem(command.itemId());
        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);

        List<SlotIndex> slotIndexes = inventoryWriter.add(
                playerInventory,
                ItemCarryPolicy.from(item),
                InventorySpec.Add.from(command)
        );

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
        Item item = itemReader.getItem(command.itemId());
        ItemCarryPolicy itemCarryPolicy = ItemCarryPolicy.from(item);
        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);
        inventoryWriter.merge(playerInventory, itemCarryPolicy, SlotIndex.of(command.from()), SlotIndex.of(command.to()));
    }

    @Transactional
    public InventoryResult.Slot split(Long playerId, InventoryCommand.Split command) {
        Item item = itemReader.getItem(command.itemId());
        ItemCarryPolicy itemCarryPolicy = ItemCarryPolicy.from(item);
        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);
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
