package online.lifeasgame.platform.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.guard.Guard;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyKeyStore implements IdempotencyKeyStore {

    private static final Duration FALLBACK_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean acquire(String key, Duration ttl) {
        String sanitized = Guard.notBlank(key, "idempotencyKey").trim();
        Duration effectiveTtl = (ttl == null || ttl.isZero() || ttl.isNegative()) ? FALLBACK_TTL : ttl;

        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(sanitized, "1", effectiveTtl);
        boolean acquired = Boolean.TRUE.equals(reserved);

        if (!acquired) {
            log.trace("Idempotency key {} rejected (already processed)", sanitized);
        }

        return acquired;
    }
}
