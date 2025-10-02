package online.lifeasgame.character.application.command;

public final class PlayerEquipmentCommand {

    private PlayerEquipmentCommand() {
    }

    public record Equip(Long slotId, Long itemInstanceId) {
        public static Equip of(Long slotId, Long itemInstanceId) {
            return new Equip(slotId, itemInstanceId);
        }
    }
}
