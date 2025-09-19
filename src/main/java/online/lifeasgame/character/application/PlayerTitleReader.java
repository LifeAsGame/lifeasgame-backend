package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerTitleReader {

    private final PlayerTitleQuery playerTitleQuery;
    private final PlayerTitleRepository playerTitleRepository;

    public List<PlayerTitleView> getPlayerTitleInfos(Long playerId) {
        return playerTitleQuery.findPlayerTitleInfos(playerId);
    }

    public boolean hasTitle(Long playerId, Long titleId) {
        return playerTitleRepository.existsByPlayerIdAndTitleId(playerId, titleId);
    }
}
