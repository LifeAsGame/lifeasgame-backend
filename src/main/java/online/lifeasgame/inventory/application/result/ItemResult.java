package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.domain.Item;

import java.util.List;
import java.util.Map;

public final class ItemResult {

    private ItemResult() {
    }

    public record Id(Long id) {
    }

    public record Deleted(Long id) {
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
        public static Detail from(Item item) {
            return new ItemResult.Detail(
                    item.getId(),
                    item.getName().value(),
                    item.getCategory().name(),
                    item.getType().name(),
                    item.getRarity().name(),
                    item.isStackable(),
                    item.maxStack(),
                    item.maxDurability(),
                    item.getBaseAttrs().attrs()
            );
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
        public static Summary from(Item item) {
            return new ItemResult.Summary(
                    item.getId(),
                    item.getName().value(),
                    item.getCategory().name(),
                    item.getType().name(),
                    item.getRarity().name(),
                    item.isStackable(),
                    item.maxStack()
            );
        }
    }

    public record Page<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <X> Page<X> from(org.springframework.data.domain.Page<X> p) {
            return new Page<>(
                    p.getContent(),
                    p.getNumber(),
                    p.getSize(),
                    p.getTotalElements(),
                    p.getTotalPages()
            );
        }
    }
}
