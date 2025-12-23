package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository {
    Achievement save(Achievement achievement);

    List<Achievement> findAll();

    List<Achievement> findByCategoryIn(List<AchievementCategory> categories);

    Optional<Achievement> findById(Long id);
}
