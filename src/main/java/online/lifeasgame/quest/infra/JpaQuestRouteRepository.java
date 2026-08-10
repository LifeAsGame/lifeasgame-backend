package online.lifeasgame.quest.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.quest.domain.QuestRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaQuestRouteRepository extends JpaRepository<QuestRoute, Long> {
    Optional<QuestRoute> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT route FROM QuestRoute route WHERE route.id = :routeId")
    Optional<QuestRoute> findByIdForUpdate(@Param("routeId") Long routeId);
}
