package online.lifeasgame.inventory.api.player.response;


import java.util.List;

public final class MailboxResponse {

    private MailboxResponse() {
    }

    public record Slot(int slot) {
    }

    public record Mail(
            int slotIndex,
            Long itemId,
            String rarity,
            int quantity,
            boolean bound
    ) {
    }

    public record Mails(List<Mail> mails) {
    }
}
