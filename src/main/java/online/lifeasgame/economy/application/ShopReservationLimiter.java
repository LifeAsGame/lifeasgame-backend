package online.lifeasgame.economy.application;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopReservationLimiter {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public boolean tryReserve(Long shopItemId, Long playerId, int quantity, Integer globalLimit, Integer perPlayerLimit, Duration ttl) {
        Duration effectiveTtl = (ttl == null || ttl.isZero() || ttl.isNegative()) ? DEFAULT_TTL : ttl;
        String itemKey = "economy:shop:" + shopItemId + ":inflight";
        String playerKey = "economy:shop:" + shopItemId + ":player:" + playerId;

        try {
            Long itemCount = redisTemplate.opsForValue().increment(itemKey, quantity);
            Long playerCount = redisTemplate.opsForValue().increment(playerKey, quantity);

            redisTemplate.expire(itemKey, effectiveTtl);
            redisTemplate.expire(playerKey, effectiveTtl);

            if (globalLimit != null && itemCount != null && itemCount > globalLimit) {
                release(shopItemId, playerId, quantity);
                return false;
            }
            if (perPlayerLimit != null && playerCount != null && playerCount > perPlayerLimit) {
                release(shopItemId, playerId, quantity);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Failed to reserve shop item {} in redis, allowing fallback", shopItemId, ex);
            release(shopItemId, playerId, quantity);
            return true;
        }
    }

    public void release(Long shopItemId, Long playerId, int quantity) {
        String itemKey = "economy:shop:" + shopItemId + ":inflight";
        String playerKey = "economy:shop:" + shopItemId + ":player:" + playerId;
        redisTemplate.opsForValue().decrement(itemKey, quantity);
        redisTemplate.opsForValue().decrement(playerKey, quantity);
    }
}
