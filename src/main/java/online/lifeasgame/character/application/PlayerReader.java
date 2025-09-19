package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerReader {

    private final PlayerRepository playerRepository;

    public Player getPlayer(Long playerId) {
        return playerRepository.findById(playerId).orElseThrow(
                () -> new DomainException(PlayerError.PLAYER_NOT_FOUND)
        );
    }

    public boolean notExists(Long playerId) {
        return playerRepository.existsById(playerId);
    }
}
