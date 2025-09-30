package online.lifeasgame.inventory.api.player.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class InventoryRequest {

    private InventoryRequest() {}

    public record Add(
            @NotNull Long itemId,
            @Min(1) int quantity,
            Map<String, Object> instanceAttrs,
            boolean bound
    ) {
    }

    public record Remove(
            @Min(0) int slotIndex,
            @Min(1) int quantity
    ) {
    }

    public record Move(
            @Min(0) int from,
            @Min(0) int to
    ) {
    }

    public record Merge(
            @Min(0) int from,
            @Min(0) int to,
            @NotNull Long itemId
    ) {
    }

    public record Split(
            @Min(0) int from,
            Integer to,
            @Min(1) int quantity,
            @NotNull Long itemId
    ) {
    }
}
