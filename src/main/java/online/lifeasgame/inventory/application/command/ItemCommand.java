package online.lifeasgame.inventory.application.command;

import java.util.Map;

public final class ItemCommand {

    private ItemCommand() {}

    public record Create(
            String name,
            String category,
            String type,
            String rarity,
            Map<String, Integer> baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurability
    ) {
    }

    public record Update(
            Long id,
            String name,
            String category,
            String type,
            String rarity,
            Map<String, Integer> baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurability
    ) {
    }
}
