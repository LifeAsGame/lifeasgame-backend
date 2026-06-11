package online.lifeasgame.quest.infra.progress;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.application.automation.QuestProgressStore;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisQuestProgressStore implements QuestProgressStore {

    private static final String KEY_PREFIX = "quest:progress:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public int increment(QuestCode questCode, Long playerId, int delta, Duration ttl) {
        String key = key(questCode, playerId);
        Long value = redisTemplate.opsForValue().increment(key, delta);
        applyTtl(key, ttl);
        return value == null ? delta : Math.toIntExact(value);
    }

    @Override
    public int set(QuestCode questCode, Long playerId, int value, Duration ttl) {
        String key = key(questCode, playerId);
        redisTemplate.opsForValue().set(key, Integer.toString(value));
        applyTtl(key, ttl);
        return value;
    }

    @Override
    public void reset(QuestCode questCode, Long playerId) {
        redisTemplate.delete(key(questCode, playerId));
    }

    private String key(QuestCode questCode, Long playerId) {
        return KEY_PREFIX + questCode.value() + ":" + playerId;
    }

    private void applyTtl(String key, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.expire(key, ttl);
    }
}
