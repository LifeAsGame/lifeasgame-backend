package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class EquipmentSlotResponse {

    private EquipmentSlotResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
    }

    public record Info(
            Long slotId,
            String code,
            String name,
            String category,
            String role
    ) {
    }
}
