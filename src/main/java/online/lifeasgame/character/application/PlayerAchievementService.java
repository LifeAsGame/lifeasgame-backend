package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.application.view.PlayerAchievementView;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.PlayerAchievement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerAchievementService {

    private final PlayerAchievementReader playerAchievementReader;
    private final PlayerAchievementWriter playerAchievementWriter;
    private final AchievementReader achievementReader;
    private final PlayerReader playerReader;

    public List<PlayerAchievementResult.Info> getPlayerAchievementInfos(Long playerId) {
        List<PlayerAchievementView> playerAchievementViews = playerAchievementReader.getViewsByPlayerId(playerId);
        return playerAchievementViews.stream()
                .map(PlayerAchievementResult.Info::from)
                .toList();
    }

    @Transactional
    public PlayerAchievementResult.Granted grantAchievement(Long playerId, Long achievementId) {
        playerReader.assertExistsById(playerId);

        Achievement achievement = achievementReader.getByIdOrThrow(achievementId);

        PlayerAchievement playerAchievement = playerAchievementWriter.create(
                PlayerAchievement.create(playerId, achievementId)
        );

        return new PlayerAchievementResult.Granted(
                playerAchievement.getPlayerId(),
                playerAchievement.getAchievementId(),
                achievement.getCode(),
                achievement.getName(),
                achievement.getCategory().name(),
                playerAchievement.getAcquiredAt()
        );
    }
}
