package online.lifeasgame.character.api.player.response;

import online.lifeasgame.character.application.result.PlayerEquipmentResult;

import java.util.List;

public class PlayerEquipmentResponse {

    private PlayerEquipmentResponse() {
    }

    public record Equipped(Long slotId, Long itemInstanceId) {
        public static Equipped of(PlayerEquipmentResult.Equipped equipped) {
            return new Equipped(
                    equipped.slotId(),
                    equipped.itemInstanceId()
            );
        }
    }

    public record Infos(List<Info> infos) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
    }

    public record Info(
            Long slotId,
            Long itemInstanceId
    ) {
        public static Info of(
                Long slotId,
                Long itemInstanceId
        ) {
            return new Info(slotId, itemInstanceId);
        }
    }
}
