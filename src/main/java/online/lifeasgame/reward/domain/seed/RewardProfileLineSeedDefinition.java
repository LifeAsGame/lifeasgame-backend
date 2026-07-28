package online.lifeasgame.reward.domain.seed;

import java.util.Objects;

public record RewardProfileLineSeedDefinition(
        RewardDefinitionContentCode definitionCode,
        int sortOrder,
        Long amountOverride
) {

    public RewardProfileLineSeedDefinition {
        Objects.requireNonNull(definitionCode, "definitionCode");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be non-negative");
        }
        if (amountOverride != null && amountOverride <= 0) {
            throw new IllegalArgumentException("amountOverride must be positive");
        }
    }
}
