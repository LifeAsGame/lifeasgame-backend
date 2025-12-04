package online.lifeasgame.economy.application.port;

import java.time.Duration;

public interface ShopReservationLimiter {

    boolean tryReserve(Long shopItemId, Long playerId, int quantity, Integer globalLimit, Integer perPlayerLimit, Duration ttl);

    void release(Long shopItemId, Long playerId, int quantity);
}
