package online.lifeasgame.inventory.api.player.request;

import jakarta.validation.constraints.Min;

public final class InventoryRequest {

    private InventoryRequest() {}

    public record Remove(
            @Min(0) int slotIndex,
            @Min(1) int quantity,
            String idempotencyKey
    ) {
    }

    public record Move(
            @Min(0) int from,
            @Min(0) int to,
            String idempotencyKey
    ) {
    }

    public record Merge(
            @Min(0) int from,
            @Min(0) int to,
            String idempotencyKey
    ) {
    }

    public record Split(
            @Min(0) int from,
            Integer to,
            @Min(1) int quantity,
            String idempotencyKey
    ) {
    }
}
