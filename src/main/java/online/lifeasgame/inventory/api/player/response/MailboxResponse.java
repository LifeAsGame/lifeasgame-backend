package online.lifeasgame.inventory.api.player.response;


import java.util.List;

public final class MailboxResponse {

    private MailboxResponse() {
    }

    public record Slot(int slot) {
        public static Slot of(int s) {
            return new Slot(s);
        }
    }

    public record Mail(
            int slotIndex,
            Long itemId,
            String rarity,
            int quantity,
            boolean bound
    ) {
        public static Mail of(
                int slotIndex,
                Long itemId,
                String rarity,
                int quantity,
                boolean bound
        ) {
            return new Mail(
                    slotIndex,
                    itemId,
                    rarity,
                    quantity,
                    bound
            );
        }
    }

    public record Mails(List<Mail> mails) {
        public static Mails of(List<Mail> mails) {
            return new Mails(mails);
        }
    }
}
