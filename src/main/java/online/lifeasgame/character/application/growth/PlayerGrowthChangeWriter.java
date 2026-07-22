package online.lifeasgame.character.application.growth;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.growth.PlayerGrowthChange;
import online.lifeasgame.character.domain.growth.repository.PlayerGrowthChangeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PlayerGrowthChangeWriter {

    private final PlayerGrowthChangeRepository repository;

    public PlayerGrowthChange saveAndFlush(PlayerGrowthChange change) {
        return repository.saveAndFlush(change);
    }
}
