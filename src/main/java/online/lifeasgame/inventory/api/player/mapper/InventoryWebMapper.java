package online.lifeasgame.inventory.api.player.mapper;

import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.player.request.InventoryRequest;
import online.lifeasgame.inventory.api.player.response.InventoryResponse;

public class InventoryWebMapper {

    private InventoryWebMapper() {
    }

    public static InventoryCommand.Add toCommand(InventoryRequest.Add request) {
        return InventoryCommand.Add.of(
                request.itemId(),
                request.quantity(),
                request.instanceAttrs(),
                request.bound()
        );
    }

    public static InventoryCommand.Remove toCommand(InventoryRequest.Remove request) {
        return InventoryCommand.Remove.of(
                request.slotIndex(),
                request.quantity()
        );
    }

    public static InventoryCommand.Move toCommand(InventoryRequest.Move request) {
        return InventoryCommand.Move.of(
                request.from(),
                request.to()
        );
    }

    public static InventoryCommand.Merge toCommand(InventoryRequest.Merge request) {
        return InventoryCommand.Merge.of(
                request.from(),
                request.to()
        );
    }

    public static InventoryCommand.Split toCommand(InventoryRequest.Split request) {
        return InventoryCommand.Split.of(
                request.from(),
                request.to(),
                request.quantity()
        );
    }

    public static InventoryResponse.Slots toSlots(InventoryResult.Slots result) {
        return InventoryResponse.Slots.of(result.slots());
    }

    public static InventoryResponse.Slot toSlot(InventoryResult.Slot result) {
        return InventoryResponse.Slot.of(result.slot());
    }

    public static InventoryResponse.Entries toEntries(InventoryResult.Entries result) {
        return InventoryResponse.Entries.of(
                result.entries().stream()
                        .map(
                                entry -> InventoryResponse.Entry.of(
                                        entry.slotIndex(),
                                        entry.itemId(),
                                        entry.rarity(),
                                        entry.quantity(),
                                        entry.bound()
                                )
                        )
                        .toList()
        );
    }
}
