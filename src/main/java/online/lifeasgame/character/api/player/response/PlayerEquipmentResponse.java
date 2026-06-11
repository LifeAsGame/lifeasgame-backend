package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class PlayerEquipmentResponse {

    private PlayerEquipmentResponse() {
    }

    public record Infos(List<Info> infos) {
    }

    public record Info(
            Long slotId,
            String slotCode,
            String slotName,
            String slotCategory,
            String slotRole,
            Long itemInstanceId
    ) {
    }

    public record Equipped(Long slotId, Long itemInstanceId) {
    }

    public record UnEquipped(Long slotId) {
    }

}
