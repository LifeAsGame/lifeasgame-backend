package online.lifeasgame.inventory.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public final class AdminItemRequest {

    private AdminItemRequest() {}

    public record Create(
            @NotBlank String name,
            @NotBlank String category,
            @NotBlank String type,
            String equipmentCompatibilityKind,
            String rarity,
            Map<String,Integer> baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurability
    ) {}

    public record Update(
            @NotBlank String name,
            @NotBlank String category,
            @NotBlank String type,
            String equipmentCompatibilityKind,
            String rarity,
            Map<String,Integer> baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurability
    ) {}
}
