package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.repository.RewardSettlementRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RewardSettlementWriter {

    private final RewardSettlementRepository repository;

    public RewardSettlement saveAndFlush(RewardSettlement settlement) {
        return repository.saveAndFlush(settlement);
    }
}
