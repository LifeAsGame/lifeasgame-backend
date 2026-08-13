package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Player;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class GrowthResult {

    private GrowthResult() {
    }

    public record Overview(Current current, List<RecentExpChange> recentExpChanges) {
    }

    public record Current(
            int level,
            long exp,
            int str,
            int agi,
            int dex,
            int intel,
            int vit,
            int luc,
            Map<String, Integer> extraStats,
            Long representativeTitleId
    ) {
        public static Current from(Player player) {
            return new Current(
                    player.getLevel().value(),
                    player.getExp().value(),
                    player.getStats().str(),
                    player.getStats().agi(),
                    player.getStats().dex(),
                    player.getStats().intel(),
                    player.getStats().vit(),
                    player.getStats().luc(),
                    player.getExtraStats().asMap(),
                    player.getTitleId()
            );
        }
    }

    public record RecentExpChange(
            Long changeId,
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int beforeLevel,
            int afterLevel,
            long beforeTotalExp,
            long afterTotalExp,
            Instant occurredAt,
            String sourceType,
            Long sourceId
    ) {
    }
}
