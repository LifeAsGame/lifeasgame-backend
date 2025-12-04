package online.lifeasgame.character.api.player.request;

public final class PlayerEquipmentRequest {

    private PlayerEquipmentRequest() {
    }

    public record Equip(
            Long itemInstanceId
    ) {
    }
}
