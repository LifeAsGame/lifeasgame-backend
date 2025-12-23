package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.domain.MailboxEntry;

import java.util.List;

public final class MailboxResult {

    private MailboxResult() {}

    public record Slot(int slot) {
    }

    public record Mail(
            int slotIndex,
            Long itemId,
            String rarity,
            int quantity,
            boolean bound
    ) {
        public static Mail from(MailboxEntry mailboxEntry) {
            return new Mail(
                    mailboxEntry.getSlotIndex().value(),
                    mailboxEntry.getItemId(),
                    mailboxEntry.getRarity().name(),
                    mailboxEntry.getQuantity().value(),
                    mailboxEntry.isBound()
            );
        }
    }

    public record Mails(List<Mail> mails) {
        public static Mails from(List<MailboxEntry> entries) {
            return new Mails(
                    entries.stream()
                            .map(Mail::from)
                            .toList()
            );
        }
    }
}
