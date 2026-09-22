package online.lifeasgame.inventory.domain.seed;

public enum ItemContentCode {
    IT_RECORD_CRYSTAL("IT_RECORD_CRYSTAL"),
    IT_FIRST_STEP_FRAGMENT("IT_FIRST_STEP_FRAGMENT");

    private final String value;

    ItemContentCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
