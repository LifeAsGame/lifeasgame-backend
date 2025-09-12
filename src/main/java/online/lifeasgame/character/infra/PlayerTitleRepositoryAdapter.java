package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerTitleRepositoryAdapter implements PlayerTitleRepository {

    private final JpaPlayerTitleRepository jpaRepository;

    public PlayerTitle save(PlayerTitle playerTitle) {
        return jpaRepository.save(playerTitle);
    }

    @Override
    public boolean existsByPlayerIdAndTitleId(Long playerId, Long titleId) {
        return jpaRepository.existsByPlayerIdAndTitleId(playerId, titleId);
    }
}
