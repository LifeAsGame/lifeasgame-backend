package online.lifeasgame.reward.domain.seed;

public enum RewardDefinitionContentCode {
    RD_EXP_20("RD_EXP_20"),
    RD_ITEM_FIRST_STEP_FRAGMENT_1("RD_ITEM_FIRST_STEP_FRAGMENT_1");

    private final String value;

    RewardDefinitionContentCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
