package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.query.PlayerAchievementQuery;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.PlayerAchievement;
import online.lifeasgame.character.domain.repository.PlayerAchievementRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PlayerAchievementRepositoryAdapter implements PlayerAchievementRepository, PlayerAchievementQuery {

    private final JpaPlayerAchievementRepository jpaRepository;

    @Override
    public PlayerAchievement save(PlayerAchievement playerAchievement) {
        return jpaRepository.save(playerAchievement);
    }

    @Override
    public void deleteByPlayerIdAndAchievementId(Long playerId, Long achievementId) {
        jpaRepository.deleteByPlayerIdAndAchievementId(playerId, achievementId);
    }

    @Override
    public List<PlayerAchievementView> findViewsByPlayerId(Long playerId) {
        return jpaRepository.findPlayerAchievementViews(playerId);
    }
}
