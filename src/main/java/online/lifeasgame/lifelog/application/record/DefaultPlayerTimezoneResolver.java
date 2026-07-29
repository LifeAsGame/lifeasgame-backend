package online.lifeasgame.lifelog.application.record;

import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class DefaultPlayerTimezoneResolver
        implements PlayerTimezoneResolver {

    public static final ZoneId FALLBACK = ZoneId.of("Asia/Seoul");

    @Override
    public ZoneId resolve(Long playerId) {
        return FALLBACK;
    }
}
