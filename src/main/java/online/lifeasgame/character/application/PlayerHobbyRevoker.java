package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerHobbyRevoker {

    private final PlayerHobbyRepository repository;

    public void revoke(Long playerId, Long hobbyId) {
        if (!repository.existsByPlayerIdAndHobbyId(playerId, hobbyId)) {
            throw new DomainException(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND);
        }

        repository.deleteByPlayerIdAndHobbyId(playerId, hobbyId);
    }
}
