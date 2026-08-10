package online.lifeasgame.quest.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum QuestError implements ErrorCode {

    INVALID_QUEST_STATUS("QUE-400-INVALID-QUEST-STATUS", "Invalid Quest status", 400),
    INVALID_QUEST_CATEGORY("QUE-400-INVALID-QUEST-CATEGORY", "Invalid Quest category", 400),
    INVALID_QUEST_SEMANTIC_CATEGORY(
            "QUE-400-INVALID-QUEST-SEMANTIC-CATEGORY",
            "Invalid Quest semantic category",
            400
    ),
    INVALID_QUEST_PROGRESS_SOURCE(
            "QUE-400-INVALID-QUEST-PROGRESS-SOURCE",
            "Invalid Quest progress source",
            400
    ),
    INVALID_QUEST_CODE("QUE-400-INVALID-QUEST-CODE", "Invalid Quest code", 400),
    INVALID_QUEST_REPEATABLE_RULE("QUE-400-INVALID-QUEST-REPEATABLE-RULE", "Invalid Quest repeatable rule", 400),
    INVALID_QUEST_REPEAT_POLICY(
            "QUE-400-INVALID-QUEST-REPEAT-POLICY",
            "Invalid Quest repeat policy",
            400
    ),
    INVALID_QUEST_TARGET_TYPE("QUE-400-INVALID-QUEST-TARGET-TYPE", "Invalid Quest target type", 400),
    QUEST_NOT_FOUND("QUE-404-QUEST-NOT-FOUND", "Quest not found", 404),
    QUEST_ACCEPTANCE_NOT_FOUND("QUE-404-QUEST-ACCEPTANCE-NOT-FOUND", "Quest acceptance not found", 404),
    QUEST_ACCEPTANCE_ALREADY_EXISTS("QUE-409-QUEST-ACCEPTANCE-ALREADY-EXISTS", "Quest account already exists", 409),
    QUEST_ACCEPTANCE_PROGRESS_NOT_ALLOWED(
            "QUE-409-QUEST-ACCEPTANCE-PROGRESS-NOT-ALLOWED",
            "Quest acceptance progress is only allowed while in progress",
            409
    ),
    QUEST_ACCEPTANCE_GOAL_REACH_NOT_ALLOWED(
            "QUE-409-QUEST-ACCEPTANCE-GOAL-REACH-NOT-ALLOWED",
            "Quest acceptance cannot reach the goal from its current status",
            409
    ),
    QUEST_ACCEPTANCE_COMPLETION_NOT_ALLOWED(
            "QUE-409-QUEST-ACCEPTANCE-COMPLETION-NOT-ALLOWED",
            "Quest acceptance can only complete after reaching its goal",
            409
    ),
    QUEST_ACCEPTANCE_CANCELLATION_NOT_ALLOWED(
            "QUE-409-QUEST-ACCEPTANCE-CANCELLATION-NOT-ALLOWED",
            "Completed quest acceptance cannot be canceled",
            409
    ),
    QUEST_ACCEPTANCE_STATUS_TRANSITION_NOT_ALLOWED(
            "QUE-409-QUEST-ACCEPTANCE-STATUS-TRANSITION-NOT-ALLOWED",
            "Quest acceptance status transition is not allowed",
            409
    ),
    QUEST_COMPLETION_POLICY_NOT_USER_CONFIRM(
            "QUE-409-QUEST-COMPLETION-POLICY-NOT-USER-CONFIRM",
            "Quest does not require user confirmation",
            409
    ),
    QUEST_MANUAL_CHECK_NOT_ALLOWED(
            "QUE-409-QUEST-MANUAL-CHECK-NOT-ALLOWED",
            "Quest does not support manual check",
            409
    ),
    QUEST_TRANSITION_TIME_REQUIRED(
            "QUE-400-QUEST-TRANSITION-TIME-REQUIRED",
            "Quest transition time is required",
            400
    ),
    QUEST_SIGNAL_CORRELATION_REQUIRED(
            "QUE-400-QUEST-SIGNAL-CORRELATION-REQUIRED",
            "Quest signal correlation is required",
            400
    ),
    QUEST_SIGNAL_CORRELATION_TOO_LONG(
            "QUE-400-QUEST-SIGNAL-CORRELATION-TOO-LONG",
            "Quest signal correlation is too long",
            400
    ),
    QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT(
            "QUE-409-QUEST-SIGNAL-RECEIPT-PAYLOAD-CONFLICT",
            "Quest signal identity was already used with a different payload",
            409
    ),
    QUEST_DEFINITION_VERSION_INVALID(
            "QUE-400-DEFINITION-VERSION-INVALID",
            "Quest definition version must be at least 1",
            400
    ),
    QUEST_DEFINITION_VERSION_DECREASE_NOT_ALLOWED(
            "QUE-400-DEFINITION-VERSION-DECREASE-NOT-ALLOWED",
            "Quest definition version cannot decrease",
            400
    ),
    QUEST_SEMANTIC_CATEGORY_REQUIRED(
            "QUE-400-SEMANTIC-CATEGORY-REQUIRED",
            "Quest semantic category is required for the final definition contract",
            400
    ),
    QUEST_PROGRESS_SOURCE_REQUIRED(
            "QUE-400-PROGRESS-SOURCE-REQUIRED",
            "Quest progress source is required for the final definition contract",
            400
    ),
    QUEST_REPEAT_POLICY_REQUIRED(
            "QUE-400-REPEAT-POLICY-REQUIRED",
            "Quest repeat policy is required for the final definition contract",
            400
    ),
    QUEST_REPEAT_CONTRACT_CONFLICT(
            "QUE-400-REPEAT-CONTRACT-CONFLICT",
            "repeatRule and repeatPolicy cannot specify different values",
            400
    ),
    QUEST_ROLE_TEMPLATE_CODE_REQUIRED(
            "QUE-400-ROLE-TEMPLATE-CODE-REQUIRED",
            "Quest role template code is required when provided",
            400
    ),
    QUEST_ROLE_TEMPLATE_CODE_TOO_LONG(
            "QUE-400-ROLE-TEMPLATE-CODE-TOO-LONG",
            "Quest role template code is too long",
            400
    ),
    QUEST_REWARD_PROFILE_CODE_REQUIRED(
            "QUE-400-REWARD-PROFILE-CODE-REQUIRED",
            "Quest reward profile code is required",
            400
    ),
    QUEST_REWARD_PROFILE_CODE_TOO_LONG(
            "QUE-400-REWARD-PROFILE-CODE-TOO-LONG",
            "Quest reward profile code is too long",
            400
    ),
    QUEST_REWARD_CONTRACT_CONFLICT(
            "QUE-400-REWARD-CONTRACT-CONFLICT",
            "Reward profile and legacy inline reward cannot be changed together",
            400
    ),
    ROUTE_NOT_FOUND(
            "QUE-404-ROUTE-NOT-FOUND",
            "Quest route not found",
            404
    ),
    PLAYER_ROUTE_NOT_FOUND(
            "QUE-404-PLAYER-ROUTE-NOT-FOUND",
            "Player quest route not found",
            404
    ),
    ROUTE_STEP_NOT_FOUND(
            "QUE-404-ROUTE-STEP-NOT-FOUND",
            "Quest route step not found",
            404
    ),
    ROUTE_ALREADY_COMPLETED(
            "QUE-409-ROUTE-ALREADY-COMPLETED",
            "Quest route is already completed",
            409
    ),
    ROUTE_STEP_NOT_CURRENT(
            "QUE-409-ROUTE-STEP-NOT-CURRENT",
            "Expected step is not the current quest route step",
            409
    ),
    ROUTE_STEP_CRITERIA_NOT_SATISFIED(
            "QUE-409-ROUTE-STEP-CRITERIA-NOT-SATISFIED",
            "Quest route step criteria are not satisfied",
            409
    ),
    ROUTE_DEFINITION_INVALID(
            "QUE-400-ROUTE-DEFINITION-INVALID",
            "Quest route definition is invalid",
            400
    )
    ;

    private final String code;
    private final String message;
    private final int status;

    QuestError(String code, String message, int status) {
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
