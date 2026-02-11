package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.AchievementCommand;
import online.lifeasgame.character.application.result.AchievementResult;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementReader achievementReader;
    private final AchievementWriter achievementWriter;

    public List<AchievementResult.Info> getAchievements(List<String> categories) {
        List<Achievement> achievements = achievementReader.getByCategories(AchievementCategory.parse(categories));
        return AchievementResult.Info.fromList(achievements);
    }

    @Transactional
    public AchievementResult.Info create(AchievementCommand.Create command) {
        Achievement achievement = achievementWriter.create(
                Achievement.create(
                        command.code(),
                        command.name(),
                        AchievementCategory.parse(command.category()),
                        command.descMd()
                )
        );

        return new AchievementResult.Info(
                achievement.getId(),
                achievement.getCode(),
                achievement.getName(),
                achievement.getCategory().name(),
                achievement.getDescMd()
        );
    }

    public AchievementResult.Info getAchievement(Long achievementId) {
        Achievement achievement = achievementReader.getByIdOrThrow(achievementId);
        return AchievementResult.Info.from(achievement);
    }

    @Transactional
    public AchievementResult.Info update(Long achievementId, AchievementCommand.Update command) {
        AchievementCategory category = AchievementCategory.parse(command.category());

        Achievement achievement = achievementReader.getByIdOrThrow(achievementId);
        achievement.update(
                command.code(),
                command.name(),
                category,
                command.descMd()
        );

        return AchievementResult.Info.from(achievement);
    }

    @Transactional
    public AchievementResult.Deleted delete(Long achievementId) {
        achievementWriter.delete(achievementId);
        return new AchievementResult.Deleted(achievementId);
    }
}
