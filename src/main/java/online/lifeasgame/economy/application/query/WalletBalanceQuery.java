package online.lifeasgame.economy.application.query;

import online.lifeasgame.economy.domain.Currency;

import java.util.List;

public interface WalletBalanceQuery {
    List<Amount> findAvailable(Long playerId);
    List<Amount> findOpenHoldTotals(Long playerId);

    record Amount(Currency currency, long amount) {
    }
}
