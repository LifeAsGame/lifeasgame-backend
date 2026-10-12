package online.lifeasgame.economy.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("예약 만료 스케줄러")
class EconomyReservationSchedulerTest {

    @Mock MarketplaceService marketplaceService;
    @Mock ShopService shopService;
    @InjectMocks EconomyReservationScheduler scheduler;

    @Test
    @DisplayName("Marketplace 만료가 실패해도 Shop 만료를 실행한다")
    void continuesWithShopAfterMarketplaceFailure() {
        doThrow(new IllegalStateException("marketplace unavailable"))
                .when(marketplaceService).expireReservations();

        scheduler.expireReservations();

        verify(shopService).expireReservations();
    }
}
