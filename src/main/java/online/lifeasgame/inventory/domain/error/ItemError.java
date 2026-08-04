package online.lifeasgame.inventory.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum ItemError implements ErrorCode {

    ITEM_NOT_FOUND("ITM-404-NOT-FOUND", "Item not found", 404),
    ITEM_ID_INVALID("ITM-400-ID-INVALID", "Item id must be positive", 400),
    ITEM_CODE_NOT_FOUND("ITM-404-CODE-NOT-FOUND", "Item code not found", 404),
    ITEM_NAME_DUP("ITM-409-NAME-DUP", "Duplicate item name", 409),
    INVALID_ITEM_CATEGORY("ITM-400-INVALID-ITEM-CATEGORY", "Invalid item category", 400),
    INVALID_ITEM_TYPE("ITM-400-INVALID-ITEM-TYPE", "Invalid item type", 400),
    INVALID_ITEM_RARITY("ITM-400-INVALID-ITEM-RARITY", "Invalid item rarity", 400),
    POLICY_CONFLICT("ITM-409-POLICY-CONFLICT",
            "Item policy change conflicts with existing stacks (reconciliation required).", 409)
    ;

    private final String code;
    private final String message;
    private final int status;

    ItemError(String c, String m, int s) {
        this.code = c;
        this.message = m;
        this.status = s;
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
