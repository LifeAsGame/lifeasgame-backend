package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Trade;
import online.lifeasgame.economy.domain.repository.TradeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class TradeWriter {

    private final TradeRepository repository;

    public Trade create(Trade trade) {
        return repository.save(trade);
    }
}
