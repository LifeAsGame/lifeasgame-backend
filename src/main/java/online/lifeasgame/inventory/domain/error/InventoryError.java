package online.lifeasgame.inventory.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum InventoryError implements ErrorCode {
    INVALID_STACK_RULE("INV-STACK-RULE","Invalid stacking rule",400),
    INVENTORY_FULL("INV-FULL","Inventory capacity exceeded",400),
    MAILBOX_FULL("INV-MAILBOX-FULL","Mailbox capacity exceeded",400),
    SLOT_OCCUPIED("INV-SLOT-OCCUPIED","Slot already occupied",400),
    SLOT_EMPTY("INV-SLOT-EMPTY","Slot empty",404),
    INVALID_SLOT("INV-INVALID-SLOT","Invalid slot index",400),
    INVALID_QUANTITY("INV-INVALID-QUANTITY","Invalid quantity",400),
    MERGE_NOT_COMPATIBLE("INV-MERGE-INCOMPATIBLE","Entries not merge-compatible",400),
    MOVE_CONFLICT("INV-MOVE-CONFLICT","Cannot move to occupied slot",400),
    NOT_ENOUGH_QUANTITY("INV-NOT-ENOUGH-QUANTITY","Not enough quantity",400),
    DURABILITY_POLICY("INV-DURABILITY-POLICY","Durability policy violation",400),
    ITEM_NOT_FOUND("INV-ITEM-NOT-FOUND","Item not found",404),
    CONTAINER_NOT_FOUND("INV-CONTAINER-NOT-FOUND","Container not found",404);
    ;

    private final String code;
    private final String message;
    private final int status;

    InventoryError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override public String code() {
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
