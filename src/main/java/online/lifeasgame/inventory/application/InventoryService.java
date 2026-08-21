package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryReader inventoryReader;
    private final ItemReader itemReader;
    private final DomainEventPublisher domainEventPublisher;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public InventoryResult.Slots add(Long playerId, InventoryCommand.Add command) {
        Item item = itemReader.getByIdOrThrow(command.itemId());
        PlayerInventory playerInventory = inventoryReader.getByPlayerIdForUpdateOrThrow(playerId);

        List<SlotIndex> slotIndexes = playerInventory.add(
                ItemCarryPolicy.from(item),
                command.quantity(),
                InstanceAttrs.of(command.instanceAttrs()),
                command.bound()
        );

        domainEventPublisher.publishAll(playerInventory.pullEvents());

        return InventoryResult.Slots.fromList(slotIndexes);
    }

    @Transactional
    public void remove(InventoryCommand.Remove command) {
        remove(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public void remove(Long playerId, InventoryCommand.Remove command) {
        PlayerInventory playerInventory = inventoryReader.getByPlayerIdForUpdateOrThrow(playerId);
        playerInventory.remove(
                SlotIndex.of(command.slotIndex()),
                command.quantity()
        );
    }

    @Transactional
    public void move(Long playerId, InventoryCommand.Move command) {
        PlayerInventory playerInventory = inventoryReader.getByPlayerIdForUpdateOrThrow(playerId);
        playerInventory.moveWithin(
                SlotIndex.of(command.from()),
                SlotIndex.of(command.to())
        );
    }

    @Transactional
    public void move(InventoryCommand.Move command) {
        move(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public void merge(Long playerId, InventoryCommand.Merge command) {
        SlotIndex from = SlotIndex.of(command.from());
        SlotIndex to = SlotIndex.of(command.to());

        PlayerInventory playerInventory = inventoryReader.getByPlayerIdForUpdateOrThrow(playerId);
        InventoryEntry fromEntry = playerInventory.getEntry(from);

        Item item = itemReader.getByIdOrThrow(fromEntry.getItemId());
        ItemCarryPolicy fromItemCarryPolicy = ItemCarryPolicy.from(item);

        playerInventory.merge(from, to, fromItemCarryPolicy);
    }

    @Transactional
    public void merge(InventoryCommand.Merge command) {
        merge(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public InventoryResult.Slot split(Long playerId, InventoryCommand.Split command) {
        PlayerInventory playerInventory = inventoryReader.getByPlayerIdForUpdateOrThrow(playerId);
        InventoryEntry inventoryEntry = playerInventory.getEntry(SlotIndex.of(command.from()));

        Item item = itemReader.getByIdOrThrow(inventoryEntry.getItemId());
        ItemCarryPolicy itemCarryPolicy = ItemCarryPolicy.from(item);

        SlotIndex slotIndex = playerInventory.split(
                SlotIndex.of(command.from()),
                SlotIndex.ofNullable(command.to()),
                command.quantity(),
                itemCarryPolicy
        );

        return new InventoryResult.Slot(slotIndex.value());
    }

    @Transactional
    public InventoryResult.Slot split(InventoryCommand.Split command) {
        return split(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                command
        );
    }
}
