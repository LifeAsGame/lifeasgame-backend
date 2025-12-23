package online.lifeasgame.inventory.application.query;

public interface InventoryStackQuery {
    long countStacksExceeding(Long itemId, int limit);
}
