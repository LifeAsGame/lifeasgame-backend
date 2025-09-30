package online.lifeasgame.inventory.api.admin.reseponse;

public final class AdminMailboxResponse {

    private AdminMailboxResponse() {}

    public record Slot(int slot) {
        public static Slot of(int s) {
            return new Slot(s);
        }
    }
}
