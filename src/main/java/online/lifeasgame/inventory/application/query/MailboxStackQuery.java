package online.lifeasgame.inventory.application.query;

public interface MailboxStackQuery {
    long countStacksExceeding(Long itemId, int limit);
}
