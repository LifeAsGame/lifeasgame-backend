package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.AchievementResult;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementReader achievementReader;

    public List<AchievementResult.AchievementInfo> getAchievements(List<String> categories) {
        List<Achievement> achievements = achievementReader.getAchievements(AchievementCategory.parse(categories));
        return AchievementResult.AchievementInfo.fromList(achievements);
    }
}
