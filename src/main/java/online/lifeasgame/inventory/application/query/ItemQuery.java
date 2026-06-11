package online.lifeasgame.inventory.application.query;

public final class ItemQuery {

    private ItemQuery() {
    }

    public record List(
            String category,
            String type,
            String rarity,
            int page,
            int size
    ) {
        public static List of(
                String category,
                String type,
                String rarity,
                int page,
                int size
        ) {
            return new List(category, type, rarity, page, size);
        }
    }
}
