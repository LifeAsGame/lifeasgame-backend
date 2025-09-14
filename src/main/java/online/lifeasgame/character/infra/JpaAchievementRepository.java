package online.lifeasgame.character.infra;

import online.lifeasgame.character.domain.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAchievementRepository extends JpaRepository<Achievement, Long> {
}
