package online.lifeasgame.quest.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum QuestError implements ErrorCode {

    INVALID_QUEST_STATUS("QUE-400-INVALID-QUEST-STATUS", "Invalid Quest status", 400)
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
