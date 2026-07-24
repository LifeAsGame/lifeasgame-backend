package online.lifeasgame.quest.application.automation;

public record QuestSignalProcessingResult(
        Long receiptId,
        Outcome outcome
) {
    public enum Outcome {
        APPLIED,
        REPLAYED
    }

    public static QuestSignalProcessingResult applied(Long receiptId) {
        return new QuestSignalProcessingResult(receiptId, Outcome.APPLIED);
    }

    public static QuestSignalProcessingResult replayed(Long receiptId) {
        return new QuestSignalProcessingResult(receiptId, Outcome.REPLAYED);
    }

    public boolean replayed() {
        return outcome == Outcome.REPLAYED;
    }
}
