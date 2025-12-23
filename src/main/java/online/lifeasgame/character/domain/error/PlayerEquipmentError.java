package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PlayerEquipmentError implements ErrorCode {
    PLAYER_EQUIPMENT_NOT_FOUND("PEQ-404-NOT-FOUND", "Player equipment not found", 404),
    ALREADY_EQUIPPED_ITEM("PEQ-409-ALREADY-EQUIPPED", "Item is already equipped", 409),
    INVALID_ITEM_INSTANCE_ID("PEQ-400-INVALID-ITEM-INSTANCE", "Item instance id is required", 400),
    ITEM_NOT_OWNED_BY_PLAYER("PEQ-403-NOT-OWNER", "Item is not owned by the player", 403),
    ITEM_NOT_COMPATIBLE_WITH_SLOT("PEQ-422-NOT-COMPATIBLE", "Item is not compatible with the slot", 422);

    private final String code;
    private final String message;
    private final int status;

    PlayerEquipmentError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int status() {
        return status;
    }
}
