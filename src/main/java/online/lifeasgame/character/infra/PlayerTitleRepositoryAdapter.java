package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PlayerTitleRepositoryAdapter implements PlayerTitleRepository, PlayerTitleQuery {

    private final JpaPlayerTitleRepository jpaRepository;

    public PlayerTitle save(PlayerTitle playerTitle) {
        return jpaRepository.save(playerTitle);
    }

    @Override
    public boolean existsByPlayerIdAndTitleId(Long playerId, Long titleId) {
        return jpaRepository.existsByPlayerIdAndTitleId(playerId, titleId);
    }

    @Override
    public long deleteByPlayerIdAndTitleId(Long playerId, Long titleId) {
        return jpaRepository.deleteByPlayerIdAndTitleId(playerId, titleId);
    }

    @Override
    public List<PlayerTitleView> findViewsByPlayerId(Long playerId) {
        return jpaRepository.findPlayerTitleViews(playerId);
    }
}
