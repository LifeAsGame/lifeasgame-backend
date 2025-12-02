package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EconomyReservationScheduler {

    private final MarketplaceService marketplaceService;
    private final ShopService shopService;

    @Scheduled(fixedDelayString = "${lifeasgame.economy.reservation-expiry-ms:60000}")
    public void expireReservations() {
        try {
            marketplaceService.expireReservations();
            shopService.expireReservations();
        } catch (Exception ex) {
            log.warn("Failed to expire economy reservations", ex);
        }
    }
}
