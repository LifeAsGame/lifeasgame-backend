package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.AchievementProgressReadApi;
import online.lifeasgame.character.application.query.PlayerAchievementQuery;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.PlayerAchievement;
import online.lifeasgame.character.domain.repository.PlayerAchievementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerAchievementRepositoryAdapter implements
        PlayerAchievementRepository,
        PlayerAchievementQuery,
        AchievementProgressReadApi {

    private final JpaPlayerAchievementRepository jpaRepository;

    @Override
    public PlayerAchievement save(PlayerAchievement playerAchievement) {
        return jpaRepository.saveAndFlush(playerAchievement);
    }

    @Override
    public void deleteByPlayerIdAndAchievementId(Long playerId, Long achievementId) {
        jpaRepository.deleteByPlayerIdAndAchievementId(playerId, achievementId);
    }

    @Override
    public List<PlayerAchievementView> findViewsByPlayerId(Long playerId) {
        return jpaRepository.findPlayerAchievementViews(playerId);
    }

    @Override
    public Optional<PlayerAchievementView> findViewByPlayerIdAndAchievementId(
            Long playerId,
            Long achievementId
    ) {
        return jpaRepository.findViewByPlayerIdAndAchievementId(
                playerId,
                achievementId
        );
    }

    @Override
    public List<RecentAchievement> recentAchievements(
            Long playerId,
            int limit
    ) {
        return jpaRepository.findRecentPlayerAchievementViews(
                        playerId,
                        PageRequest.of(0, limit)
                ).stream()
                .map(view -> new RecentAchievement(
                        view.getAchievementId(),
                        view.getCode(),
                        view.getName(),
                        view.getCategory().name(),
                        view.getDescMd(),
                        view.getAcquiredAt()
                ))
                .toList();
    }
}
