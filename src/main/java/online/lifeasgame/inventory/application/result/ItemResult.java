package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.domain.Item;

import java.util.List;
import java.util.Map;

public final class ItemResult {

    private ItemResult() {}

    public record Id(Long id) {
        public static ItemResult.Id of(Long id) {
            return new ItemResult.Id(id);
        }
    }

    public record Deleted(Long id) {
        public static ItemResult.Deleted of(Long id) {
            return new ItemResult.Deleted(id);
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
        public static Detail of(
                Long id,
                String name,
                String category,
                String type,
                String rarity,
                boolean stackable,
                int i,
                Integer maxDurability,
                Map<String, Integer> attrs
        ) {
            return new Detail(
                    id,
                    name,
                    category,
                    type,
                    rarity,
                    stackable,
                    i,
                    maxDurability,
                    attrs
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
        public static Summary of(
                Long id,
                String name,
                String category,
                String type,
                String rarity,
                boolean stackable,
                int i
        ) {
            return new Summary(
                    id,
                    name,
                    category,
                    type,
                    rarity,
                    stackable,
                    i
            );
        }

        public static Summary from(Item i) {
            return new ItemResult.Summary(
                    i.getId(),
                    i.getName().value(),
                    i.getCategory().name(),
                    i.getType().name(),
                    i.getRarity().name(),
                    i.isStackable(),
                    i.maxStack()
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
        public static <X> Page<X> of(org.springframework.data.domain.Page<X> p) {
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
