package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerTitleRegistrar {

    private final PlayerTitleRepository repository;

    public PlayerTitle register(PlayerTitle playerTitle) {
        return repository.save(playerTitle);
    }
}
