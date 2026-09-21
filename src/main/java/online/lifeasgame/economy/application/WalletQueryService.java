package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.application.query.WalletBalanceQuery;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletQueryService {

    private final WalletBalanceQuery query;

    @Transactional(readOnly = true)
    public EconomyResult.WalletSummary wallet(Long playerId) {
        var available = new EnumMap<Currency, Long>(Currency.class);
        var held = new EnumMap<Currency, Long>(Currency.class);
        query.findAvailable(playerId).forEach(row -> available.put(row.currency(), row.amount()));
        query.findOpenHoldTotals(playerId).forEach(row -> held.put(row.currency(), row.amount()));
        return new EconomyResult.WalletSummary(List.of(Currency.GOLD, Currency.GEM).stream()
                .map(currency -> new EconomyResult.CurrencyBalance(currency,
                        available.getOrDefault(currency, 0L), held.getOrDefault(currency, 0L)))
                .toList());
    }
}
