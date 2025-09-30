package online.lifeasgame.inventory.api.player.response;


import java.util.List;
import java.util.Map;

public final class ItemResponse {

    private ItemResponse() {
    }

    public record Id(Long id) {
        public static Id of(Long id) {
            return new Id(id);
        }
    }

    public record Summary(
            Long id,
            String name,
            String category,
            String type,
            String rarity,
            boolean stackable,
            int maxStack
    ) {
        public static Summary of(
                Long id,
                String name,
                String category,
                String type,
                String rarity,
                boolean stackable,
                int maxStack
        ) {
            return new Summary(
                    id,
                    name,
                    category,
                    type,
                    rarity,
                    stackable,
                    maxStack
            );
        }
    }

    public record Detail(
            Long id,
            String name,
            String category,
            String type,
            String rarity,
            boolean stackable,
            int maxStack,
            Integer maxDurability,
            Map<String, Integer> baseAttrs
    ) {
    }

    public record Page<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
