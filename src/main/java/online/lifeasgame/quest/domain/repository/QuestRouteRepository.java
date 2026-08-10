package online.lifeasgame.quest.domain.repository;

import online.lifeasgame.quest.domain.QuestRoute;

import java.util.List;
import java.util.Optional;

public interface QuestRouteRepository {
    List<QuestRoute> findAll();

    Optional<QuestRoute> findById(Long routeId);

    Optional<QuestRoute> findByIdForUpdate(Long routeId);

    Optional<QuestRoute> findByCode(String code);
}
