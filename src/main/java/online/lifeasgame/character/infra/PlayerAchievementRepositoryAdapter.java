package online.lifeasgame.character.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.AchievementProgressReadApi;
import online.lifeasgame.character.application.query.PlayerAchievementQuery;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.PlayerAchievement;
import online.lifeasgame.character.domain.repository.PlayerAchievementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerAchievementRepositoryAdapter implements
        PlayerAchievementRepository,
        PlayerAchievementQuery,
        AchievementProgressReadApi {

    private final JpaPlayerAchievementRepository jpaRepository;
    private final AwardReceiptAcquiredAtQuery acquiredAtQuery;

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
        var grantTimes = acquiredAtQuery.achievements(playerId);
        return jpaRepository.findPlayerAchievementViews(playerId).stream()
                .map(view -> withGrantTime(view, grantTimes.get(view.getAchievementId())))
                .toList();
    }

    @Override
    public Optional<PlayerAchievementView> findViewByPlayerIdAndAchievementId(
            Long playerId,
            Long achievementId
    ) {
        var grantTime = acquiredAtQuery.achievements(playerId).get(achievementId);
        return jpaRepository.findViewByPlayerIdAndAchievementId(
                playerId,
                achievementId
        ).map(view -> withGrantTime(view, grantTime));
    }

    @Override
    public List<RecentAchievement> recentAchievements(
            Long playerId,
            int limit
    ) {
        var grantTimes = acquiredAtQuery.achievements(playerId);
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
                        grantTimes.getOrDefault(view.getAchievementId(), view.getAcquiredAt())
                ))
                .toList();
    }

    private static PlayerAchievementView withGrantTime(PlayerAchievementView view, Instant grantTime) {
        if (grantTime == null) return view;
        return new GrantTimeView(view, grantTime);
    }

    private record GrantTimeView(PlayerAchievementView delegate, Instant acquiredAt)
            implements PlayerAchievementView {
        public Long getAchievementId() { return delegate.getAchievementId(); }
        public String getCode() { return delegate.getCode(); }
        public String getName() { return delegate.getName(); }
        public AchievementCategory getCategory() { return delegate.getCategory(); }
        public String getDescMd() { return delegate.getDescMd(); }
        public Instant getAcquiredAt() { return acquiredAt; }
    }
}
