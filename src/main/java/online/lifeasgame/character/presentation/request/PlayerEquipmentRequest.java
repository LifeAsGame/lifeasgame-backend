package online.lifeasgame.character.presentation.request;

public class PlayerEquipmentRequest {

    private PlayerEquipmentRequest() {
    }

    public record EquipEquipment(
            Long itemInstanceId
    ) {
        public static EquipEquipment of(Long itemInstanceId) {
            return new EquipEquipment(itemInstanceId);
        }
    }
}
