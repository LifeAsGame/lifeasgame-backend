package online.lifeasgame.economy.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Trade;
import online.lifeasgame.economy.domain.repository.TradeRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TradeRepositoryAdapter implements TradeRepository {

    private final JpaTradeRepository jpaTradeRepository;

    @Override
    public Trade save(Trade trade) {
        return jpaTradeRepository.save(trade);
    }

    @Override
    public java.util.List<Trade> findByBuyerPlayerIdOrSellerPlayerId(Long buyerId, Long sellerId) {
        return jpaTradeRepository.findByBuyerPlayerIdOrSellerPlayerId(buyerId, sellerId);
    }
}
