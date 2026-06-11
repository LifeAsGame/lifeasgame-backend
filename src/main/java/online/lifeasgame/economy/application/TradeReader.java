package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Trade;
import online.lifeasgame.economy.domain.repository.TradeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class TradeReader {

    private final TradeRepository repository;

    public List<Trade> findByPlayer(Long playerId) {
        return repository.findByBuyerPlayerIdOrSellerPlayerId(playerId, playerId);
    }
}
