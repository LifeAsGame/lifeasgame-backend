package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.PlayerAchievement;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerAchievementService {

    private final PlayerAchievementReader playerAchievementReader;
    private final AchievementReader achievementReader;
    private final PlayerAchievementWriter playerAchievementWriter;
    private final PlayerReader playerReader;

    public List<PlayerAchievementResult.PlayerAchievementInfo> getPlayerAchievementInfos(Long playerId) {
        List<PlayerAchievementView> playerAchievementViews = playerAchievementReader.getPlayerAchievementInfos(playerId);
        return playerAchievementViews.stream()
                .map(PlayerAchievementResult.PlayerAchievementInfo::from)
                .toList();
    }

    @Transactional
    public PlayerAchievementResult.GrantedAchievement grantAchievement(Long playerId, Long achievementId) {
        if (playerReader.notExists(playerId)) {
            throw new DomainException(PlayerError.PLAYER_NOT_FOUND);
        }

        Achievement achievement = achievementReader.getAchievement(achievementId);

        PlayerAchievement saved = playerAchievementWriter.grantAchievement(
                PlayerAchievement.create(
                        playerId,
                        achievementId
                )
        );

        return PlayerAchievementResult.GrantedAchievement.of(
                saved.getPlayerId(),
                saved.getAchievementId(),
                achievement.getCode(),
                achievement.getName(),
                achievement.getCategory().name(),
                saved.getAcquiredAt()
        );
    }
}
