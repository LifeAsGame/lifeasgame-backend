package online.lifeasgame.quest.domain;

import java.util.Map;

public record RewardStats(Map<String, Integer> stats) {
    public RewardStats {
        stats = (stats == null) ? Map.of() : Map.copyOf(stats);
    }

    public static RewardStats empty() {
        return new RewardStats(Map.of());
    }
}
