package online.lifeasgame.character.application.growth;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.growth.PlayerGrowthChange;
import online.lifeasgame.character.domain.growth.repository.PlayerGrowthChangeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.MANDATORY)
public class PlayerGrowthChangeReader {

    private final PlayerGrowthChangeRepository repository;

    public Optional<PlayerGrowthChange> findByRewardLineId(Long rewardLineId) {
        return repository.findByRewardLineId(rewardLineId);
    }
}
