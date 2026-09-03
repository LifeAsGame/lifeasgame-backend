package online.lifeasgame.economy.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Trade;
import online.lifeasgame.economy.domain.repository.TradeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TradeRepositoryAdapter implements TradeRepository {

    private final JpaTradeRepository jpaTradeRepository;

    @Override
    public Trade save(Trade trade) {
        return jpaTradeRepository.save(trade);
    }

    @Override
    public Optional<Trade> findById(Long id) {
        return jpaTradeRepository.findById(id);
    }

    @Override
    public java.util.List<Trade> findByBuyerPlayerIdOrSellerPlayerId(Long buyerId, Long sellerId) {
        return jpaTradeRepository.findByBuyerPlayerIdOrSellerPlayerId(buyerId, sellerId);
    }
}
