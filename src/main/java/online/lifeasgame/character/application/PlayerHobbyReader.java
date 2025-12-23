package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerHobbyQuery;
import online.lifeasgame.character.application.view.PlayerHobbyView;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerHobbyReader {

    private final PlayerHobbyQuery query;
    private final PlayerHobbyRepository repository;

    public List<PlayerHobbyView> getViewsByPlayerId(Long playerId) {
        return query.findViewsByPlayerId(playerId);
    }

    public PlayerHobby getByPlayerIdAndHobbyId(Long playerId, Long hobbyId) {
        return repository.findByPlayerIdAndHobbyId(playerId, hobbyId)
                .orElseThrow(() -> new DomainException(PlayerHobbyError.PLAYER_HOBBY_NOT_FOUND));
    }
}
