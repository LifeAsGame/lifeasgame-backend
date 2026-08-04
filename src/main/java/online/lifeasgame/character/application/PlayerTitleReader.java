package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.error.PlayerTitleError;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerTitleReader {

    private final PlayerTitleQuery query;
    private final PlayerTitleRepository repository;

    public List<PlayerTitleView> getViewsByPlayerId(Long playerId) {
        return query.findViewsByPlayerId(playerId);
    }

    public void assertHasTitle(Long playerId, Long titleId) {
        if (!repository.existsByPlayerIdAndTitleId(playerId, titleId)) {
            throw new DomainException(PlayerTitleError.PLAYER_TITLE_NOT_FOUND);
        }
    }
}
