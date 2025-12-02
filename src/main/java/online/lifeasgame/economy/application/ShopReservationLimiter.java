package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ShopReservationLimiter {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private static final Logger log = LoggerFactory.getLogger(ShopReservationLimiter.class);

    private final StringRedisTemplate redisTemplate;

    public boolean tryReserve(Long shopItemId, Long playerId, int quantity, Integer globalLimit, Integer perPlayerLimit, Duration ttl) {
        Duration effectiveTtl = (ttl == null || ttl.isZero() || ttl.isNegative()) ? DEFAULT_TTL : ttl;
        String itemKey = "economy:shop:" + shopItemId + ":inflight";
        String playerKey = "economy:shop:" + shopItemId + ":player:" + playerId;

        Long itemCount = null;
        Long playerCount = null;
        boolean itemUpdated = false;
        boolean playerUpdated = false;

        try {
            itemCount = redisTemplate.opsForValue().increment(itemKey, quantity);
            itemUpdated = true;
            playerCount = redisTemplate.opsForValue().increment(playerKey, quantity);
            playerUpdated = true;

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
            if (itemUpdated) {
                redisTemplate.opsForValue().decrement(itemKey, quantity);
            }
            if (playerUpdated) {
                redisTemplate.opsForValue().decrement(playerKey, quantity);
            }
            return true;
        }
    }

    public void release(Long shopItemId, Long playerId, int quantity) {
        String itemKey = "economy:shop:" + shopItemId + ":inflight";
        String playerKey = "economy:shop:" + shopItemId + ":player:" + playerId;
        Long itemCount = redisTemplate.opsForValue().decrement(itemKey, quantity);
        Long playerCount = redisTemplate.opsForValue().decrement(playerKey, quantity);
        if (itemCount != null && itemCount <= 0) {
            redisTemplate.delete(itemKey);
        }
        if (playerCount != null && playerCount <= 0) {
            redisTemplate.delete(playerKey);
        }
    }
}
