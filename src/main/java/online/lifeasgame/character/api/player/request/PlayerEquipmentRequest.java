package online.lifeasgame.character.api.player.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class PlayerEquipmentRequest {

    private PlayerEquipmentRequest() {
    }

    public record Equip(
            @NotNull @Min(1) Long itemInstanceId
    ) {
    }
}
