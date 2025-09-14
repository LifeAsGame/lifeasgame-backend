package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.AdminPlayerAchievementResult;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.PlayerAchievement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class AdminPlayerAchievementService {

    private final AchievementReader achievementReader;
    private final PlayerAchievementWriter playerAchievementWriter;

    @Transactional
    public AdminPlayerAchievementResult.GrantedAchievement grantAchievement(Long playerId, Long achievementId) {
        Achievement achievement = achievementReader.getAchievement(achievementId);

        PlayerAchievement saved = playerAchievementWriter.grantAchievement(
                PlayerAchievement.create(
                        playerId,
                        achievementId
                )
        );

        return AdminPlayerAchievementResult.GrantedAchievement.of(
                saved.getPlayerId(),
                saved.getAchievementId(),
                achievement.getCode(),
                achievement.getName(),
                achievement.getCategory().name(),
                saved.getAcquiredAt()
        );
    }
}
