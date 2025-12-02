package online.lifeasgame.economy.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Trade;
import online.lifeasgame.economy.domain.repository.TradeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TradeReader {

    private final TradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public List<Trade> findByPlayer(Long playerId) {
        return tradeRepository.findByBuyerPlayerIdOrSellerPlayerId(playerId, playerId);
    }
}
