package online.lifeasgame.reward.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum RewardError implements ErrorCode {

    REWARD_DEFINITION_NOT_FOUND(
            "RWD-404-DEFINITION-NOT-FOUND", "Reward definition not found", 404
    ),
    REWARD_DEFINITION_CODE_REQUIRED(
            "RWD-400-DEFINITION-CODE-REQUIRED", "Reward definition code is required", 400
    ),
    REWARD_DEFINITION_CODE_TOO_LONG(
            "RWD-400-DEFINITION-CODE-TOO-LONG", "Reward definition code is too long", 400
    ),
    REWARD_DEFINITION_NAME_REQUIRED(
            "RWD-400-DEFINITION-NAME-REQUIRED", "Reward definition name is required", 400
    ),
    REWARD_DEFINITION_NAME_TOO_LONG(
            "RWD-400-DEFINITION-NAME-TOO-LONG", "Reward definition name is too long", 400
    ),
    REWARD_PROFILE_NOT_FOUND(
            "RWD-404-PROFILE-NOT-FOUND", "Reward profile not found", 404
    ),
    REWARD_PROFILE_CODE_REQUIRED(
            "RWD-400-PROFILE-CODE-REQUIRED", "Reward profile code is required", 400
    ),
    REWARD_PROFILE_CODE_TOO_LONG(
            "RWD-400-PROFILE-CODE-TOO-LONG", "Reward profile code is too long", 400
    ),
    REWARD_PROFILE_NAME_REQUIRED(
            "RWD-400-PROFILE-NAME-REQUIRED", "Reward profile name is required", 400
    ),
    REWARD_PROFILE_NAME_TOO_LONG(
            "RWD-400-PROFILE-NAME-TOO-LONG", "Reward profile name is too long", 400
    ),
    REWARD_PROFILE_INACTIVE(
            "RWD-409-PROFILE-INACTIVE", "Reward profile is inactive", 409
    ),
    REWARD_LINE_TYPE_REQUIRED(
            "RWD-400-LINE-TYPE-REQUIRED", "Reward type is required", 400
    ),
    REWARD_AMOUNT_MUST_BE_POSITIVE(
            "RWD-400-AMOUNT-NOT-POSITIVE", "Reward amount must be positive", 400
    ),
    REWARD_EXP_ITEM_ID_NOT_ALLOWED(
            "RWD-400-EXP-ITEM-ID-NOT-ALLOWED", "EXP reward must not have an item id", 400
    ),
    REWARD_ITEM_ID_REQUIRED(
            "RWD-400-ITEM-ID-REQUIRED", "Item reward requires an item id", 400
    ),
    REWARD_ITEM_ID_MUST_BE_POSITIVE(
            "RWD-400-ITEM-ID-NOT-POSITIVE", "Reward item id must be positive", 400
    ),
    REWARD_ITEM_QUANTITY_MUST_BE_POSITIVE(
            "RWD-400-ITEM-QUANTITY-NOT-POSITIVE", "Item reward quantity must be positive", 400
    ),
    REWARD_PROFILE_LINE_REQUIRED(
            "RWD-400-PROFILE-LINE-REQUIRED", "Reward profile line requires a profile", 400
    ),
    REWARD_LINE_TARGET_REQUIRED(
            "RWD-400-LINE-TARGET-REQUIRED", "Reward profile line requires a definition", 400
    ),
    REWARD_LINE_SORT_ORDER_MUST_BE_NON_NEGATIVE(
            "RWD-400-LINE-SORT-ORDER-NEGATIVE", "Reward line sort order must be non-negative", 400
    ),
    REWARD_LINE_SORT_ORDER_DUPLICATED(
            "RWD-409-LINE-SORT-ORDER-DUPLICATED", "Reward line sort order must be unique", 409
    ),
    REWARD_AMOUNT_OVERRIDE_MUST_BE_POSITIVE(
            "RWD-400-AMOUNT-OVERRIDE-NOT-POSITIVE", "Reward amount override must be positive", 400
    ),
    REWARD_SETTLEMENT_NOT_FOUND(
            "RWD-404-SETTLEMENT-NOT-FOUND", "Reward settlement not found", 404
    ),
    REWARD_SETTLEMENT_PLAYER_ID_REQUIRED(
            "RWD-400-SETTLEMENT-PLAYER-ID-REQUIRED", "Reward settlement player id must be positive", 400
    ),
    REWARD_SETTLEMENT_SOURCE_TYPE_REQUIRED(
            "RWD-400-SETTLEMENT-SOURCE-TYPE-REQUIRED", "Reward settlement source type is required", 400
    ),
    REWARD_SETTLEMENT_SOURCE_ID_REQUIRED(
            "RWD-400-SETTLEMENT-SOURCE-ID-REQUIRED", "Reward settlement source id must be positive", 400
    ),
    REWARD_SETTLEMENT_PROFILE_REQUIRED(
            "RWD-400-SETTLEMENT-PROFILE-REQUIRED", "Reward settlement requires a reward profile", 400
    ),
    REWARD_SETTLEMENT_PROFILE_ID_REQUIRED(
            "RWD-400-SETTLEMENT-PROFILE-ID-REQUIRED", "Reward settlement requires a persisted reward profile", 400
    ),
    REWARD_SETTLEMENT_PROFILE_LINES_REQUIRED(
            "RWD-400-SETTLEMENT-PROFILE-LINES-REQUIRED", "Reward settlement requires at least one profile line", 400
    ),
    REWARD_SETTLEMENT_DEFINITION_ID_REQUIRED(
            "RWD-400-SETTLEMENT-DEFINITION-ID-REQUIRED", "Reward settlement line requires a persisted reward definition", 400
    ),
    REWARD_SETTLEMENT_LINE_NOT_FOUND(
            "RWD-404-SETTLEMENT-LINE-NOT-FOUND", "Reward settlement line not found", 404
    ),
    REWARD_SETTLEMENT_LINE_NOT_EXP(
            "RWD-409-SETTLEMENT-LINE-NOT-EXP", "Reward settlement line is not an EXP reward", 409
    ),
    REWARD_SETTLEMENT_LINE_ALREADY_FAILED(
            "RWD-409-SETTLEMENT-LINE-ALREADY-FAILED", "Failed reward settlement line cannot succeed", 409
    ),
    REWARD_SETTLEMENT_SUCCEEDED_LINE_CANNOT_FAIL(
            "RWD-409-SETTLEMENT-SUCCEEDED-LINE-CANNOT-FAIL", "Succeeded reward settlement line cannot fail", 409
    ),
    REWARD_SETTLEMENT_FAILURE_CODE_REQUIRED(
            "RWD-400-SETTLEMENT-FAILURE-CODE-REQUIRED", "Failed reward settlement line requires an error code", 400
    ),
    REWARD_SETTLEMENT_EXP_GROWTH_INCONSISTENT(
            "RWD-409-SETTLEMENT-EXP-GROWTH-INCONSISTENT", "EXP line and player growth change are inconsistent", 409
    );

    private final String code;
    private final String message;
    private final int status;

    RewardError(String code, String message, int status) {
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
