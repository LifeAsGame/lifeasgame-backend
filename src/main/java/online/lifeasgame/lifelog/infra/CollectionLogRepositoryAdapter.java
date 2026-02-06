package online.lifeasgame.lifelog.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.repository.CollectionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CollectionLogRepositoryAdapter implements CollectionLogRepository {

    private final CollectionLogJpaRepository jpa;

    @Override
    public CollectionLog save(CollectionLog log) {
        return jpa.save(log);
    }

    @Override
    public Optional<CollectionLog> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<CollectionLog> findByIdAndPlayerId(Long id, Long playerId) {
        return jpa.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public List<CollectionLog> findByPlayerId(Long playerId, int limit) {
        return jpa.findRecentWithTags(playerId, PageRequest.of(0, limit));
    }

    @Override
    public List<CollectionLog> search(
            Long playerId,
            CollectionCategory category,
            String titleLike,
            int page,
            int size
    ) {
        Page<Long> idPage = jpa.searchIds(
                playerId,
                category,
                titleLike,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
        if (idPage.isEmpty()) return List.of();

        // 2) fetch join by ids
        List<Long> ids = idPage.getContent();
        return jpa.findAllWithTagsByIdIn(ids);
    }

    @Override
    public void deleteByIdAndPlayerId(Long collectionId, Long playerId) {
        jpa.deleteByIdAndPlayerId(collectionId, playerId);
    }
}
