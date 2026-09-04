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

    private final PlayerRepository repository;

    public Player getByIdOrThrow(Long playerId) {
        return repository.findById(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Player getByIdForUpdateOrThrow(Long playerId) {
        return repository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));
    }

    public void assertExistsById(Long playerId) {
        if (!repository.existsById(playerId)) {
            throw new DomainException(PlayerError.PLAYER_NOT_FOUND);
        }
    }

    public Player getByUserIdOrThrow(Long userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new DomainException(PlayerError.PLAYER_NOT_FOUND));
    }

    public Player getByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Player getByUserIdForUpdateOrThrow(Long userId) {
        return repository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new DomainException(
                        PlayerError.PLAYER_NOT_FOUND
                ));
    }
}
