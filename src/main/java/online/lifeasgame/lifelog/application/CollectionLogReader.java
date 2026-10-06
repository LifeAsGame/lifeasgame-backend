package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.repository.CollectionLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class CollectionLogReader {

    private final CollectionLogRepository repository;

    public CollectionLog getByIdAndPlayerIdOrThrow(Long id, Long playerId) {
        return repository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new DomainException(LifeLogError.COLLECTION_NOT_FOUND));
    }

    public List<CollectionLog> recent(Long playerId, int limit) {
        return repository.findByPlayerId(playerId, limit);
    }

    public List<CollectionLog> search(
            Long playerId,
            String category,
            String titleLike,
            int page,
            int size,
            Long personalCategoryId,
            boolean unclassified
    ) {
        CollectionCategory collectionCategory = CollectionCategory.parseNullable(category);
        return repository.search(playerId, collectionCategory, titleLike, page, size, personalCategoryId, unclassified);
    }

    public List<CollectionLog> search(Long playerId, String category, String titleLike, int page, int size) {
        return search(playerId, category, titleLike, page, size, null, false);
    }
}
