package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EconomyReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EconomyReservationScheduler.class);

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
