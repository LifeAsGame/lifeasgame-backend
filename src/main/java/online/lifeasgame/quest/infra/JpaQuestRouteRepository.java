package online.lifeasgame.quest.infra;

import online.lifeasgame.quest.domain.QuestRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaQuestRouteRepository extends JpaRepository<QuestRoute, Long> {
    Optional<QuestRoute> findByCode(String code);
}
