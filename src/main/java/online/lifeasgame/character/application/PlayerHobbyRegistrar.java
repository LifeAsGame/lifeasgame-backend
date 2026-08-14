package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerHobbyRegistrar {

    private final PlayerHobbyRepository repository;

    public PlayerHobby register(PlayerHobby playerHobby) {
        return repository.save(playerHobby);
    }
}
