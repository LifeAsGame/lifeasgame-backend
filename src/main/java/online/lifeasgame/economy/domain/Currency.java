package online.lifeasgame.economy.domain;

import online.lifeasgame.core.lang.EnumParsers;

public enum Currency {
    GOLD, GEM

    public static Currency parseOptional(String raw, Currency fallback) {
        return EnumParsers.parseOptional(Currency.class, raw).orElse(fallback);
    }
}
