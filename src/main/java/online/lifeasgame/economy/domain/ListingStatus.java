package online.lifeasgame.economy.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.economy.domain.error.EconomyError;

import java.util.List;

public enum ListingStatus {
    OPEN, RESERVED, SOLD, CANCELED, EXPIRED;

    public static ListingStatus parse(String raw) {
        return EnumParsers.parseStrict(
                ListingStatus.class,
                raw,
                EconomyError.INVALID_LISTING_STATUS,
                "Listing statuse"
        );
    }

    public static ListingStatus parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<ListingStatus> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                ListingStatus.class,
                raw,
                EconomyError.INVALID_LISTING_STATUS,
                "Listing statuses"
        );
    }
}
