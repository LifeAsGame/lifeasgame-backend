package online.lifeasgame.character.infra;

import java.util.Collection;
import java.util.List;
import online.lifeasgame.character.domain.Achievement;
import online.lifeasgame.character.domain.AchievementCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByCategoryIn(Collection<AchievementCategory> categories);
}
