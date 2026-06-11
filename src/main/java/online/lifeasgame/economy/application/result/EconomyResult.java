package online.lifeasgame.economy.application.result;

import online.lifeasgame.economy.domain.Listing;
import online.lifeasgame.economy.domain.ShopItem;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.Trade;

import java.time.Instant;
import java.util.List;

public final class EconomyResult {
    
    private EconomyResult() {
    }

    public record ListingId(Long id) {
    }

    public record Reservation(
            String reservationToken,
            String holdId,
            Instant expiresAt
    ) {
    }

    public record ShopPurchaseId(Long id) {
    }

    public record WalletBalance(long amount, String currency) {
    }

    public record ShopItems(List<ShopItemView> items) {
        public static ShopItems fromList(List<ShopItem> shopItems) {
            return new ShopItems(
                    shopItems.stream()
                        .map(EconomyResult.ShopItemView::from)
                        .toList()
            );
        }
    }

    public record ListingSummaries(List<ListingSummary> listings) {
        public static ListingSummaries fromList(List<Listing> listings) {
            return new ListingSummaries (
                    listings.stream()
                    .map(ListingSummary::from)
                    .toList()
            );
        }
    }

    public record PlayerListings(List<ListingSummary> listings) {
        public static PlayerListings fromList(List<Listing> listings) {
            return new PlayerListings (
                    listings.stream()
                            .map(ListingSummary::from)
                            .toList()
            );
        }
    }

    public record PlayerReservations(List<ListingReservation> reservations) {
        public static PlayerReservations fromList(List<Listing> listings) {
            return new PlayerReservations (
                    listings.stream()
                            .map(ListingReservation::from)
                            .toList()
            );
        }

    }

    public record ShopPurchases(List<ShopPurchaseView> purchases) {
        public static ShopPurchases fromList(List<ShopPurchase> shopPurchases) {
            return new ShopPurchases (
                    shopPurchases.stream()
                            .map(ShopPurchaseView::from)
                            .toList()
            );
        }
    }

    public record Trades(List<TradeSummary> trades) {
        public static Trades fromList(List<Trade> trades) {
            return new Trades (
                    trades.stream()
                            .map(TradeSummary::from)
                            .toList()
            );
        }
    }

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

    public record ShopItemView(
            Long id,
            Long itemId,
            long price,
            String currency,
            boolean available,
            Integer globalStockLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSec
    ) {
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

    public record ShopPurchaseView(
            Long id,
            Long shopItemId,
            Long playerId,
            Integer quantity,
            String status,
            String reservationToken,
            Instant reservationExpiresAt
    ) {
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
