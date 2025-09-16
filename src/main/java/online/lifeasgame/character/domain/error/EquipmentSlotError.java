package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum EquipmentSlotError implements ErrorCode {
    INVALID_EQUIPMENT_SLOT_CATEGORY("EQS-400-INVALID-EQUIPMENT_SLOT-CATEGORY", "Invalid equipment slot category", 400),
    INVALID_EQUIPMENT_SLOT_ROLE("EQS-400-INVALID-EQUIPMENT_SLOT-ROLE", "Invalid equipment slot role", 400),
    EQUIPMENT_SLOT_NOT_FOUND("EQS-404-NOT_FOUND", "Equipment slot not found", 404),
    ;

    private final String code;
    private final String message;
    private final int status;

    EquipmentSlotError(String code, String message, int status) {
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
