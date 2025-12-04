package online.lifeasgame.economy.infra;

import online.lifeasgame.economy.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTradeRepository extends JpaRepository<Trade, Long> {
    java.util.List<Trade> findByBuyerPlayerIdOrSellerPlayerId(Long buyerId, Long sellerId);
}
