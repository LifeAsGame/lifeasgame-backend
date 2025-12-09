package online.lifeasgame.inventory.api.player.response;

import java.util.List;

public final class InventoryResponse {

    private InventoryResponse() {}

    public record Slots(List<Integer> slots) {
    }

    public record Slot(int slot) {
    }

    public record Entry(
            int slotIndex,
            Long itemId,
            String rarity,
            int quantity,
            boolean bound
    ) {
    }

    public record Entries(List<Entry> entries) {
    }
}
