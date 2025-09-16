package online.lifeasgame.character.application.command;

public class PlayerEquipmentCommand {

    private PlayerEquipmentCommand() {
    }

    public record EquipEquipment(Long slotId, Long itemInstanceId) {
        public static EquipEquipment of(Long slotId, Long itemInstanceId) {
            return new EquipEquipment(slotId, itemInstanceId);
        }
    }
}
