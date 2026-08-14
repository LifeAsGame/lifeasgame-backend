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
@Transactional(propagation = Propagation.MANDATORY)
class PlayerTitleRevoker {

    private final PlayerTitleRepository repository;

    public void revoke(Long playerId, Long titleId) {
        if (repository.deleteByPlayerIdAndTitleId(playerId, titleId) == 0) {
            throw new DomainException(PlayerTitleError.PLAYER_TITLE_NOT_FOUND);
        }
    }
}
