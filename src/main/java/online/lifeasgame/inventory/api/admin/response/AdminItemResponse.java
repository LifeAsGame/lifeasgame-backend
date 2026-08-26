package online.lifeasgame.inventory.api.admin.response;

import java.util.List;
import java.util.Map;

public final class AdminItemResponse {

    private AdminItemResponse() {}

    public record Id(Long id) {
    }

    public record Deleted(Long id) {
    }

    public record Summary(
            Long id,
            String code,
            String name,
            String category,
            String type,
            String rarity,
            boolean stackable,
            int maxStack
    ) {}

    public record Detail(
            Long id,
            String code,
            String name,
            String category,
            String type,
            String rarity,
            boolean stackable,
            int maxStack,
            Integer maxDurability,
            Map<String, Integer> baseAttrs
    ) {}

    public record Page<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
