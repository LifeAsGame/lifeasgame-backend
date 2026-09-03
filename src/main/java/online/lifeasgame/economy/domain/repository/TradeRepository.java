package online.lifeasgame.economy.domain.repository;

import online.lifeasgame.economy.domain.Trade;

import java.util.Optional;

public interface TradeRepository {
    Trade save(Trade trade);
    Optional<Trade> findById(Long id);
    java.util.List<Trade> findByBuyerPlayerIdOrSellerPlayerId(Long buyerId, Long sellerId);
}
