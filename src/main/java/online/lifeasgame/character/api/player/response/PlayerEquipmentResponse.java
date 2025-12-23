package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class PlayerEquipmentResponse {

    private PlayerEquipmentResponse() {
    }

    public record Equipped(Long slotId, Long itemInstanceId) {
    }

    public record Infos(List<Info> infos) {
    }

    public record Info(
            Long slotId,
            Long itemInstanceId
    ) {
    }
}
