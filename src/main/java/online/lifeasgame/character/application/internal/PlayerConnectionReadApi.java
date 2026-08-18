package online.lifeasgame.character.application.internal;

import java.util.Map;
import java.util.Set;

public interface PlayerConnectionReadApi {

    Map<Long, PlayerSummary> findAllByPlayerIds(Set<Long> playerIds);

    record PlayerSummary(
            Long playerId,
            String name,
            String job,
            int level
    ) {
    }
}
