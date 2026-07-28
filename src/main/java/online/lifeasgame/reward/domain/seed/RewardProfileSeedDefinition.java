package online.lifeasgame.reward.domain.seed;

import online.lifeasgame.reward.domain.RewardProfileStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record RewardProfileSeedDefinition(
        RewardProfileContentCode code,
        String name,
        RewardProfileStatus status,
        List<RewardProfileLineSeedDefinition> lines
) {

    public RewardProfileSeedDefinition {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        List<RewardProfileLineSeedDefinition> copiedLines =
                List.copyOf(Objects.requireNonNull(lines, "lines"));
        var sortOrders = new HashSet<Integer>();
        if (copiedLines.stream().map(RewardProfileLineSeedDefinition::sortOrder)
                .anyMatch(sortOrder -> !sortOrders.add(sortOrder))) {
            throw new IllegalArgumentException("line sortOrder must be unique");
        }
        lines = copiedLines;
    }
}
