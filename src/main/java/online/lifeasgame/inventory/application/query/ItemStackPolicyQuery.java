package online.lifeasgame.inventory.application.query;

public interface ItemStackPolicyQuery {
    long countInventoryStacksExceeding(Long itemId, int limit);
    long countMailboxStacksExceeding(Long itemId, int limit);

    default long countTotalStacksExceeding(Long itemId, int limit) {
        return countInventoryStacksExceeding(itemId, limit)
                + countMailboxStacksExceeding(itemId, limit);
    }
}
