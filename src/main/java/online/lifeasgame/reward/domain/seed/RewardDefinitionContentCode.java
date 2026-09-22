package online.lifeasgame.reward.domain.seed;

public enum RewardDefinitionContentCode {
    RD_ADVENTURE_GOLD("RD_ADVENTURE_GOLD"),
    RD_RECORD_CRYSTAL("RD_RECORD_CRYSTAL"),
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
