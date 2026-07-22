package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.repository.RewardSettlementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RewardSettlementWriter {

    private final RewardSettlementRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RewardSettlement saveAndFlush(RewardSettlement settlement) {
        return repository.saveAndFlush(settlement);
    }
}
