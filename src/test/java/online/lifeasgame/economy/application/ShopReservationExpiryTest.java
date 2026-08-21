package online.lifeasgame.economy.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.application.port.ShopReservationLimiter;
import online.lifeasgame.economy.domain.Currency;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.ReservationToken;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.Wallet;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Shop reservation expiry")
class ShopReservationExpiryTest {

    private static final Long PLAYER_ID = 296L;

    @Mock
    private ShopItemReader shopItemReader;
    @Mock
    private ShopItemWriter shopItemWriter;
    @Mock
    private ShopPurchaseReader shopPurchaseReader;
    @Mock
    private ShopPurchaseWriter shopPurchaseWriter;
    @Mock
    private WalletReader walletReader;
    @Mock
    private WalletWriter walletWriter;
    @Mock
    private IdempotencyKeyStore idempotencyKeyStore;
    @Mock
    private ShopReservationLimiter shopReservationLimiter;
    @Mock
    private DomainEventPublisher eventPublisher;
    @InjectMocks
    private ShopService service;

    @Test
    @DisplayName("ShopPurchase owning hold만 환불하고 unrelated overdue hold는 유지한다")
    void expiresOnlyPurchaseHold() {
        Instant createdAt = Instant.now().minusSeconds(30);
        Wallet wallet = Wallet.open(PLAYER_ID);
        wallet.deposit(Money.of(100L, Currency.GOLD));
        String owningHold = wallet.placeHold(
                Money.of(40L, Currency.GOLD),
                "shop",
                createdAt,
                1
        );
        String unrelatedHold = wallet.placeHold(
                Money.of(20L, Currency.GOLD),
                "listing",
                createdAt,
                1
        );
        ShopPurchase purchase = ShopPurchase.request(
                10L,
                PLAYER_ID,
                1,
                Money.of(40L, Currency.GOLD)
        );
        purchase.reserve(
                ReservationToken.newToken(),
                createdAt.plusSeconds(1),
                owningHold
        );
        given(shopPurchaseReader.findExpiringBefore(any()))
                .willReturn(List.of(purchase));
        given(walletReader.getByOwnerIdForUpdate(PLAYER_ID))
                .willReturn(Optional.of(wallet));

        service.expireReservations();

        assertThat(purchase.getStatus()).isEqualTo(ShopPurchase.Status.EXPIRED);
        assertThat(wallet.getBalance(Currency.GOLD).available()).isEqualTo(80L);
        wallet.cancelHold(unrelatedHold);
        assertThat(wallet.getBalance(Currency.GOLD).available()).isEqualTo(100L);
        verify(shopReservationLimiter).release(10L, PLAYER_ID, 1);
        verify(shopPurchaseWriter).create(purchase);
    }
}
