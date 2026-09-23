package online.lifeasgame.economy.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.error.EconomyError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WalletBalanceTest {
    @Test
    @DisplayName("잔액 상한을 넘는 입금은 기존 잔액을 보존하고 도메인 오류로 거부한다")
    void rejectsOverflowWithoutChangingBalance() {
        Wallet wallet = Wallet.open(1L);
        wallet.deposit(Money.of(Long.MAX_VALUE - 99, Currency.GOLD));
        assertThatThrownBy(() -> wallet.deposit(Money.of(100, Currency.GOLD)))
                .isInstanceOfSatisfying(DomainException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(EconomyError.WALLET_BALANCE_OVERFLOW));
        assertThat(wallet.getBalance().available()).isEqualTo(Long.MAX_VALUE - 99);
        wallet.deposit(Money.of(99, Currency.GOLD));
        assertThat(wallet.getBalance().available()).isEqualTo(Long.MAX_VALUE);
    }
}
