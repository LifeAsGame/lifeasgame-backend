package online.lifeasgame.inventory.api.admin.response;

import java.util.List;

public final class AdminInventoryResponse {

    private AdminInventoryResponse() {}

    public record Slots(List<Integer> slots) {
        public static Slots of(List<Integer> s) {
            return new Slots(s);
        }
    }
}
