package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AdminAchievementCommand;
import online.lifeasgame.character.application.result.AdminAchievementResult;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAchievementService {

    private final AchievementWriter achievementWriter;

    @Transactional
    public AdminAchievementResult.AchievementInfo create(AdminAchievementCommand.CreateAchievement command) {
        Achievement achievement = achievementWriter.create(
                Achievement.of(
                        command.code(),
                        command.name(),
                        AchievementCategory.parse(command.category()),
                        command.descMd()
                )
        );

        return AdminAchievementResult.AchievementInfo.of(
                achievement.getCode(),
                achievement.getName(),
                achievement.getCategory().name(),
                achievement.getDescMd()
        );
    }
}
