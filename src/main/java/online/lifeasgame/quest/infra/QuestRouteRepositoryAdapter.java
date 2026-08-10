package online.lifeasgame.quest.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.QuestRoute;
import online.lifeasgame.quest.domain.repository.QuestRouteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuestRouteRepositoryAdapter implements QuestRouteRepository {

    private final JpaQuestRouteRepository jpaRepository;

    @Override
    public List<QuestRoute> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<QuestRoute> findById(Long routeId) {
        return jpaRepository.findById(routeId);
    }

    @Override
    public Optional<QuestRoute> findByIdForUpdate(Long routeId) {
        return jpaRepository.findByIdForUpdate(routeId);
    }

    @Override
    public Optional<QuestRoute> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }
}
