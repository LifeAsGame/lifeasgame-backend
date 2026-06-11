package online.lifeasgame.quest.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum QuestError implements ErrorCode {

    INVALID_QUEST_STATUS("QUE-400-INVALID-QUEST-STATUS", "Invalid Quest status", 400),
    INVALID_QUEST_CATEGORY("QUE-400-INVALID-QUEST-CATEGORY", "Invalid Quest category", 400),
    INVALID_QUEST_CODE("QUE-400-INVALID-QUEST-CODE", "Invalid Quest code", 400),
    INVALID_QUEST_REPEATABLE_RULE("QUE-400-INVALID-QUEST-REPEATABLE-RULE", "Invalid Quest repeatable rule", 400),
    INVALID_QUEST_TARGET_TYPE("QUE-400-INVALID-QUEST-TARGET-TYPE", "Invalid Quest target type", 400),
    QUEST_ACCEPTANCE_ALREADY_EXISTS("QUE-409-QUEST-ACCEPTANCE-ALREADY-EXISTS", "Quest account already exists", 409)
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
