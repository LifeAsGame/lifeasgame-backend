package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.domain.MailboxEntry;

import java.util.List;

public final class MailboxResult {

    private MailboxResult() {}

    public record Slot(int slot) {
        public static Slot of(int s) {
            return new Slot(s);
        }
    }

    public record Mail(int slotIndex, Long itemId, String rarity, int quantity, boolean bound) {
        public static Mail from(MailboxEntry e) {
            return new Mail(
                    e.getSlotIndex().value(),
                    e.getItemId(),
                    e.getRarity().name(),
                    e.getQuantity().value(),
                    e.isBound()
            );
        }
    }

    public record Mails(List<Mail> mails) {
        public static Mails of(List<Mail> list) {
            return new Mails(list);
        }

        public static Mails from(List<MailboxEntry> entries) {
            return new Mails(
                    entries.stream()
                            .map(Mail::from)
                            .toList()
            );
        }
    }
}
