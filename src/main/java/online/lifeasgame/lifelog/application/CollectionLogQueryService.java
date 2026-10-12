package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.query.CollectionQuery;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.LifeLogCategoryKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionLogQueryService {

    private final CollectionLogReader collectionLogReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final LifeLogCategoryService categoryService;

    public List<CollectionResult.Info> recent(int limit) {
        return recent(currentPlayerAccessor.currentPlayerIdOrThrow(), limit);
    }

    public List<CollectionResult.Info> recent(Long playerId, int limit) {
        return collectionLogReader.recent(playerId, limit).stream()
                .map(CollectionResult.Info::from)
                .toList();
    }

    public List<CollectionResult.Info> search(CollectionQuery.Search query) {
        return search(currentPlayerAccessor.currentPlayerIdOrThrow(), query);
    }

    public List<CollectionResult.Info> search(Long playerId, CollectionQuery.Search query) {
        categoryService.validateFilter(playerId, LifeLogCategoryKind.COLLECTION,
                query.personalCategoryId(), query.unclassified());
        return collectionLogReader.search(
                        playerId,
                        query.category(),
                        query.titleLike(),
                        query.page(),
                        query.size(),
                        query.personalCategoryId(),
                        query.unclassified()
                ).stream()
                .map(CollectionResult.Info::from)
                .toList();
    }

    public CollectionResult.Info getCollection(Long collectionId) {
        return getCollection(currentPlayerAccessor.currentPlayerIdOrThrow(), collectionId);
    }

    public CollectionResult.Info getCollection(Long playerId, Long collectionId) {
        CollectionLog collectionLog = collectionLogReader
                .getByIdAndPlayerIdOrThrow(collectionId, playerId);
        return CollectionResult.Info.from(collectionLog);
    }
}
