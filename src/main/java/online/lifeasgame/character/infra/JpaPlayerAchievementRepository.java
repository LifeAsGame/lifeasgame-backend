package online.lifeasgame.character.infra;

import online.lifeasgame.character.domain.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {
}
