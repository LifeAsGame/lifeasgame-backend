package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerWriter {

    private final PlayerRepository repository;

    public Long create(Player player) {
        Player saved = repository.save(player);
        saved.markRegistered();
        return saved.getId();
    }
}
