package online.lifeasgame.economy.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.economy.domain.error.EconomyError;

public enum Currency {
    GOLD,
    GEM;

    public static Currency parseStrict(String raw) {
        return EnumParsers.parseStrict(
                Currency.class,
                raw,
                EconomyError.INVALID_CURRENCY,
                "currency"
        );
    }

    public static Currency parseOptional(String raw, Currency fallback) {
        return EnumParsers.parseOptional(Currency.class, raw)
                .orElse(fallback);
    }
}
