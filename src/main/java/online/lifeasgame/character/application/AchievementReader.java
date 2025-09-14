package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import online.lifeasgame.character.domain.repository.AchievementRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AchievementReader {

    private final AchievementRepository repository;

    public List<Achievement> getAchievements(List<AchievementCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByCategoryIn(categories);
    }
}
