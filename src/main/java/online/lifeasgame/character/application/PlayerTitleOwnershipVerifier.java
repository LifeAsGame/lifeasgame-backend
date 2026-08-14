package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.error.PlayerTitleError;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.MANDATORY)
class PlayerTitleOwnershipVerifier {

    private final PlayerTitleRepository repository;

    public void verifyOwned(Long playerId, Long titleId) {
        if (!repository.existsByPlayerIdAndTitleId(playerId, titleId)) {
            throw new DomainException(PlayerTitleError.PLAYER_TITLE_NOT_FOUND);
        }
    }
}
