package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.character.domain.repository.PlayerRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerReader {

    private final PlayerRepository repository;

    public Player getByIdOrThrow(Long playerId) {
        return repository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));
    }

    public void assertExistsById(Long playerId) {
        if (!repository.existsById(playerId)) {
            throw new DomainException(PlayerError.PLAYER_NOT_FOUND);
        }
    }

    public void assertNotExistsByUserId(Long userId) {
        if (repository.existsByUserId(userId)) {
            throw new DomainException(PlayerError.PLAYER_ALREADY_EXISTS);
        }
    }

    public List<Player> getByUserIdOrThrow(Long userId) {
        return repository.findByUserId(userId);
    }
}
