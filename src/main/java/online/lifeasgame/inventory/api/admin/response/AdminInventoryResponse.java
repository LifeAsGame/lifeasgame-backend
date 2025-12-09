package online.lifeasgame.inventory.api.admin.response;

import java.util.List;

public final class AdminInventoryResponse {

    private AdminInventoryResponse() {}

    public record Slots(List<Integer> slots) {
    }
}
