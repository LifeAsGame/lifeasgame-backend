package online.lifeasgame.quest.application;

import java.time.ZoneId;

public interface PlayerTimezoneResolver {

    ZoneId resolve(Long playerId);
}
