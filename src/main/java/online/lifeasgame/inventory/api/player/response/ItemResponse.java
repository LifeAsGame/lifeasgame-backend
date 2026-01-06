package online.lifeasgame.inventory.api.player.response;


import java.util.List;
import java.util.Map;

public final class ItemResponse {

    private ItemResponse() {
    }

    public record Id(Long id) {
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

    public record Meta(
            List<String> categories,
            List<String> types,
            List<String> rarities
    ) {}
}
