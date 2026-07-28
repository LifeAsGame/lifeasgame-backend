package online.lifeasgame.reward.domain.seed;

public enum RewardProfileContentCode {
    RP_NONE("RP_NONE"),
    RP_EXP_10("RP_EXP_10"),
    RP_EXP_30("RP_EXP_30"),
    RP_EXP_AND_ITEM_FIRST_STEP_20("RP_EXP_AND_ITEM_FIRST_STEP_20");

    private final String value;

    RewardProfileContentCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
