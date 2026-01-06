package online.lifeasgame.inventory.api.player.mapper;

import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.player.request.InventoryRequest;
import online.lifeasgame.inventory.api.player.response.InventoryResponse;

public class InventoryWebMapper {

    private InventoryWebMapper() {
    }

    public static InventoryResponse.Entries toEntries(InventoryResult.Entries result) {
        return new InventoryResponse.Entries(
                result.entryViews().stream()
                        .map(InventoryWebMapper::toEntry)
                        .toList()
        );
    }

    private static InventoryResponse.Entry toEntry(InventoryResult.Entry e) {
        return new InventoryResponse.Entry(
                e.itemInstanceId(),
                e.slotIndex(),
                e.itemId(),
                e.itemName(),
                e.category(),
                e.type(),
                e.rarity(),
                e.stackable(),
                e.maxStack(),
                e.quantity(),
                e.bound(),
                e.durability(),
                e.instanceAttrs()
        );
    }

    public static InventoryCommand.Add toAddCommand(InventoryRequest.Add request) {
        return new InventoryCommand.Add(
                request.itemId(),
                request.quantity(),
                request.instanceAttrs(),
                request.bound()
        );
    }

    public static InventoryResponse.Slots toSlots(InventoryResult.Slots result) {
        return new InventoryResponse.Slots(result.slots());
    }

    public static InventoryCommand.Move toMoveCommand(InventoryRequest.Move request) {
        return new InventoryCommand.Move(
                request.from(),
                request.to()
        );
    }

    public static InventoryCommand.Merge toMergeCommand(InventoryRequest.Merge request) {
        return new InventoryCommand.Merge(
                request.from(),
                request.to()
        );
    }

    public static InventoryCommand.Split toSplitCommand(InventoryRequest.Split request) {
        return new InventoryCommand.Split(
                request.from(),
                request.to(),
                request.quantity()
        );
    }

    public static InventoryResponse.Slot toSlot(InventoryResult.Slot result) {
        return new InventoryResponse.Slot(result.slot());
    }

    public static InventoryCommand.Remove toRemoveCommand(InventoryRequest.Remove request) {
        return new InventoryCommand.Remove(
                request.slotIndex(),
                request.quantity()
        );
    }
}
