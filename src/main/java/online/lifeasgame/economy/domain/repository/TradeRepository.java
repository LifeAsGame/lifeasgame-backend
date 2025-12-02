package online.lifeasgame.economy.domain.repository;

import online.lifeasgame.economy.domain.Trade;

public interface TradeRepository {
    Trade save(Trade trade);
    java.util.List<Trade> findByBuyerPlayerIdOrSellerPlayerId(Long buyerId, Long sellerId);
}
