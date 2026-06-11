package online.lifeasgame.platform.idempotency;

import java.time.Duration;

public interface IdempotencyKeyStore {

    /**
     * Attempts to acquire the given idempotency key for the specified TTL.
     *
     * @param key the unique key to reserve
     * @param ttl retention duration for the key; implementations may apply a sensible default when {@code null}
     * @return {@code true} if the key was reserved and the caller should continue processing, {@code false} otherwise
     */
    boolean acquire(String key, Duration ttl);
}
