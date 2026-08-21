package online.lifeasgame.economy.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Wallet targeted hold expiry")
class WalletHoldExpiryTest {

    @Test
    @DisplayName("지정한 overdue hold만 환불하고 다른 overdue hold는 OPEN으로 유지한다")
    void expiresOnlyOwningHold() {
        Wallet wallet = Wallet.open(296L);
        wallet.deposit(Money.of(200L, Currency.GOLD));
        Instant createdAt = Instant.now().minusSeconds(30);
        String owningHold = wallet.placeHold(
                Money.of(80L, Currency.GOLD),
                "listing",
                createdAt,
                1
        );
        String unrelatedHold = wallet.placeHold(
                Money.of(60L, Currency.GOLD),
                "shop",
                createdAt,
                1
        );

        wallet.expireHold(owningHold, Instant.now());

        assertThat(wallet.getBalance(Currency.GOLD).available())
                .isEqualTo(140L);
        wallet.cancelHold(unrelatedHold);
        assertThat(wallet.getBalance(Currency.GOLD).available())
                .isEqualTo(200L);
    }
}
