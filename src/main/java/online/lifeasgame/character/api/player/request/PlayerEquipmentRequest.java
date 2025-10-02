package online.lifeasgame.character.api.player.request;

public class PlayerEquipmentRequest {

    private PlayerEquipmentRequest() {
    }

    public record Equip(
            Long itemInstanceId
    ) {
        public static Equip of(Long itemInstanceId) {
            return new Equip(itemInstanceId);
        }
    }
}
