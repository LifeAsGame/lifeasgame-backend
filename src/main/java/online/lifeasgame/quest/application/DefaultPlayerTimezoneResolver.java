package online.lifeasgame.quest.application;

import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component("questPlayerTimezoneResolver")
public class DefaultPlayerTimezoneResolver
        implements PlayerTimezoneResolver {

    public static final ZoneId FALLBACK = ZoneId.of("Asia/Seoul");

    @Override
    public ZoneId resolve(Long playerId) {
        return FALLBACK;
    }
}
