package online.lifeasgame.character.domain.repository;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;

public interface AchievementRepository {
    Achievement save(Achievement achievement);

    List<Achievement> findAll();

    List<Achievement> findByCategoryIn(List<AchievementCategory> categories);

    Optional<Achievement> findById(Long id);
}
