package online.lifeasgame.lifelog.application.record;

import java.time.ZoneId;

public interface PlayerTimezoneResolver {

    ZoneId resolve(Long playerId);
}
