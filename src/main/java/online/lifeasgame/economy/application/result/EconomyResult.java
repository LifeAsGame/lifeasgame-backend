package online.lifeasgame.economy.application.result;

import java.time.Instant;
import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.Trade;

public final class EconomyResult {
    private EconomyResult() {}

    public record ListingId(Long id) { public static ListingId of(Long id) { return new ListingId(id); } }
    public record Reservation(String reservationToken, String holdId, Instant expiresAt) {
        public static Reservation of(String token, String holdId, Instant expiresAt) {
            return new Reservation(token, holdId, expiresAt);
        }
    }
    public record TradeId(Long id) { public static TradeId of(Long id) { return new TradeId(id); } }
    public record ShopPurchaseId(Long id) { public static ShopPurchaseId of(Long id) { return new ShopPurchaseId(id); } }
    public record WalletBalance(long amount, String currency) { public static WalletBalance of(long amount, String currency) { return new WalletBalance(amount, currency); } }
    public record ShopItems(java.util.List<ShopItemView> items) { }
    public record Listings(java.util.List<ListingSummary> listings) { }
    public record PlayerListings(java.util.List<ListingSummary> listings) { }
    public record PlayerReservations(java.util.List<ListingReservation> reservations) { }
    public record ShopPurchases(java.util.List<ShopPurchaseView> purchases) { }
    public record Trades(java.util.List<TradeSummary> trades) { }

    public record ListingSummary(Long id, Long itemId, Long sellerId, long price, String currency, String status) {
        public static ListingSummary from(Listing listing) {
            return new ListingSummary(
                    listing.getId(),
                    listing.getItemId(),
                    listing.getSellerPlayerId(),
                    listing.getPrice().amount(),
                    listing.getPrice().currency().name(),
                    listing.getStatus().name()
            );
        }
    }

    public record TradeSummary(Long id, Long listingId, Long buyerId, Long sellerId, long price, String currency) {
        public static TradeSummary from(Trade trade) {
            return new TradeSummary(
                    trade.getId(),
                    trade.getListingId(),
                    trade.getBuyerPlayerId(),
                    trade.getSellerPlayerId(),
                    trade.getPrice().amount(),
                    trade.getPrice().currency().name()
            );
        }
    }

    public record ShopReservation(String reservationToken, Instant expiresAt) {
        public static ShopReservation from(ShopPurchase purchase) {
            return new ShopReservation(purchase.getReservationToken(), purchase.getReservationExpiresAt());
        }
    }

    public record ListingReservation(Long listingId, Long itemId, long price, String currency, Instant expiresAt) {
        public static ListingReservation from(Listing listing) {
            return new ListingReservation(
                    listing.getId(),
                    listing.getItemId(),
                    listing.getPrice().amount(),
                    listing.getPrice().currency().name(),
                    listing.getReservationExpiresAt()
            );
        }
    }

    public record ShopItemView(Long id, Long itemId, long price, String currency, boolean available,
                               Integer globalStockLimit, Integer perPlayerLimit, Integer reservationTtlSec) {
        public static ShopItemView from(online.lifeasgame.economy.domain.ShopItem shopItem) {
            return new ShopItemView(
                    shopItem.getId(),
                    shopItem.getItemId(),
                    shopItem.getPrice().amount(),
                    shopItem.getPrice().currency().name(),
                    shopItem.isAvailable(),
                    shopItem.getGlobalStockLimit(),
                    shopItem.getPerPlayerLimit(),
                    shopItem.getReservationTtlSec()
            );
        }
    }

    public record ShopPurchaseView(Long id, Long shopItemId, Long playerId, Integer quantity, String status,
                                   String reservationToken, Instant reservationExpiresAt) {
        public static ShopPurchaseView from(ShopPurchase purchase) {
            return new ShopPurchaseView(
                    purchase.getId(),
                    purchase.getShopItemId(),
                    purchase.getPlayerId(),
                    purchase.getQuantity(),
                    purchase.getStatus().name(),
                    purchase.getReservationToken(),
                    purchase.getReservationExpiresAt()
            );
        }
    }
}
