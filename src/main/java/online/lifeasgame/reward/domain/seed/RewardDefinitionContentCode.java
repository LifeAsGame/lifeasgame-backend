package online.lifeasgame.reward.domain.seed;

public enum RewardDefinitionContentCode {
    EXP_PLAYER("EXP_PLAYER"),
    ITEM_DEFINITION("ITEM_DEFINITION");

    private final String value;

    RewardDefinitionContentCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
