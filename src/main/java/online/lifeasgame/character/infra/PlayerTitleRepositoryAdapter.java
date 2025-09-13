package online.lifeasgame.character.infra;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerTitleQuery;
import online.lifeasgame.character.application.view.PlayerTitleView;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerTitleRepositoryAdapter implements PlayerTitleRepository, PlayerTitleQuery {

    private final JpaPlayerTitleRepository jpaRepository;

    public PlayerTitle save(PlayerTitle playerTitle) {
        return jpaRepository.save(playerTitle);
    }

    @Override
    public List<PlayerTitleView> findPlayerTitleInfos(Long playerId) {
        return jpaRepository.findPlayerTitleViews(playerId);
    }
}
