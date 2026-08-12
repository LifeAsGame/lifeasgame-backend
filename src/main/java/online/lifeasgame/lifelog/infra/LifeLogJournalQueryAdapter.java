package online.lifeasgame.lifelog.infra;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.query.LifeLogJournalQuery;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static online.lifeasgame.lifelog.domain.QCollectionLog.collectionLog;
import static online.lifeasgame.lifelog.domain.QExerciseLog.exerciseLog;
import static online.lifeasgame.lifelog.domain.QMediaLog.mediaLog;
import static online.lifeasgame.lifelog.domain.record.QLifeLogRecord.lifeLogRecord;

@Repository
@RequiredArgsConstructor
public class LifeLogJournalQueryAdapter implements LifeLogJournalQuery {

    private final JPAQueryFactory queryFactory;

    @Override
    public CanonicalPage findPage(
            Long playerId,
            Long primaryRoleId,
            LifeLogSubtype subtype,
            int page,
            int size
    ) {
        List<CanonicalRecord> content = queryFactory
                .select(Projections.constructor(
                        CanonicalRecord.class,
                        lifeLogRecord.id,
                        lifeLogRecord.sourceType,
                        lifeLogRecord.sourceId,
                        lifeLogRecord.subtype,
                        lifeLogRecord.entryMode,
                        lifeLogRecord.reflectionScope,
                        lifeLogRecord.periodKey,
                        lifeLogRecord.primaryRoleId,
                        lifeLogRecord.roleEventId,
                        lifeLogRecord.occurredAt
                ))
                .from(lifeLogRecord)
                .where(
                        lifeLogRecord.playerId.eq(playerId),
                        primaryRoleIdEq(primaryRoleId),
                        subtypeEq(subtype)
                )
                .orderBy(
                        lifeLogRecord.occurredAt.desc(),
                        lifeLogRecord.id.desc()
                )
                .offset((long) page * size)
                .limit(size)
                .fetch();
        Long count = queryFactory
                .select(lifeLogRecord.count())
                .from(lifeLogRecord)
                .where(
                        lifeLogRecord.playerId.eq(playerId),
                        primaryRoleIdEq(primaryRoleId),
                        subtypeEq(subtype)
                )
                .fetchOne();
        long total = count == null ? 0L : count;
        int totalPages = (int) Math.ceil(total / (double) size);
        return new CanonicalPage(content, page, size, total, totalPages);
    }

    @Override
    public List<CanonicalRecord> findRecent(Long playerId, int limit) {
        return queryFactory
                .select(Projections.constructor(
                        CanonicalRecord.class,
                        lifeLogRecord.id,
                        lifeLogRecord.sourceType,
                        lifeLogRecord.sourceId,
                        lifeLogRecord.subtype,
                        lifeLogRecord.entryMode,
                        lifeLogRecord.reflectionScope,
                        lifeLogRecord.periodKey,
                        lifeLogRecord.primaryRoleId,
                        lifeLogRecord.roleEventId,
                        lifeLogRecord.occurredAt
                ))
                .from(lifeLogRecord)
                .where(lifeLogRecord.playerId.eq(playerId))
                .orderBy(
                        lifeLogRecord.occurredAt.desc(),
                        lifeLogRecord.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public List<RoleCount> countByPrimaryRole(
            Long playerId,
            Instant windowStart,
            Instant windowEnd
    ) {
        NumberExpression<Long> recordCount = lifeLogRecord.id.count();
        return queryFactory
                .select(lifeLogRecord.primaryRoleId, recordCount)
                .from(lifeLogRecord)
                .where(
                        lifeLogRecord.playerId.eq(playerId),
                        lifeLogRecord.occurredAt.goe(windowStart),
                        lifeLogRecord.occurredAt.loe(windowEnd)
                )
                .groupBy(lifeLogRecord.primaryRoleId)
                .orderBy(
                        recordCount.desc(),
                        lifeLogRecord.primaryRoleId.asc()
                )
                .fetch()
                .stream()
                .map(row -> new RoleCount(
                        row.get(lifeLogRecord.primaryRoleId),
                        row.get(recordCount)
                ))
                .toList();
    }

    @Override
    public Optional<CanonicalRecord> findOwned(
            Long playerId,
            Long lifeLogId
    ) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        CanonicalRecord.class,
                        lifeLogRecord.id,
                        lifeLogRecord.sourceType,
                        lifeLogRecord.sourceId,
                        lifeLogRecord.subtype,
                        lifeLogRecord.entryMode,
                        lifeLogRecord.reflectionScope,
                        lifeLogRecord.periodKey,
                        lifeLogRecord.primaryRoleId,
                        lifeLogRecord.roleEventId,
                        lifeLogRecord.occurredAt
                ))
                .from(lifeLogRecord)
                .where(
                        lifeLogRecord.id.eq(lifeLogId),
                        lifeLogRecord.playerId.eq(playerId)
                )
                .fetchOne());
    }

    @Override
    public Map<SourceKey, LifeLogJournalResult.Preview> loadPreviews(
            Long playerId,
            List<CanonicalRecord> records
    ) {
        Map<SourceKey, LifeLogJournalResult.Preview> previews =
                new HashMap<>();
        loadCollectionPreviews(
                playerId,
                sourceIds(records, LifeLogSourceType.COLLECTION),
                previews
        );
        loadExercisePreviews(
                playerId,
                sourceIds(records, LifeLogSourceType.EXERCISE),
                previews
        );
        loadMediaPreviews(
                playerId,
                sourceIds(records, LifeLogSourceType.MEDIA),
                previews
        );
        return previews;
    }

    @Override
    public Optional<LifeLogJournalResult.Source> loadSource(
            Long playerId,
            CanonicalRecord record
    ) {
        return switch (record.sourceType()) {
            case COLLECTION -> loadCollectionSource(
                    playerId,
                    record.sourceId()
            );
            case EXERCISE -> loadExerciseSource(
                    playerId,
                    record.sourceId()
            );
            case MEDIA -> loadMediaSource(playerId, record.sourceId());
        };
    }

    private void loadCollectionPreviews(
            Long playerId,
            Set<Long> sourceIds,
            Map<SourceKey, LifeLogJournalResult.Preview> previews
    ) {
        if (sourceIds.isEmpty()) {
            return;
        }
        List<Tuple> rows = queryFactory
                .select(
                        collectionLog.id,
                        collectionLog.category,
                        collectionLog.title.value,
                        collectionLog.quantity.value
                )
                .from(collectionLog)
                .where(
                        collectionLog.playerId.eq(playerId),
                        collectionLog.id.in(sourceIds)
                )
                .fetch();
        rows.forEach(row -> previews.put(
                new SourceKey(
                        LifeLogSourceType.COLLECTION,
                        row.get(collectionLog.id)
                ),
                new LifeLogJournalResult.CollectionPreview(
                        row.get(collectionLog.category).name(),
                        row.get(collectionLog.title.value),
                        row.get(collectionLog.quantity.value)
                )
        ));
    }

    private void loadExercisePreviews(
            Long playerId,
            Set<Long> sourceIds,
            Map<SourceKey, LifeLogJournalResult.Preview> previews
    ) {
        if (sourceIds.isEmpty()) {
            return;
        }
        List<Tuple> rows = queryFactory
                .select(
                        exerciseLog.id,
                        exerciseLog.category,
                        exerciseLog.metrics.durationMinutes,
                        exerciseLog.metrics.distanceKm,
                        exerciseLog.metrics.calories,
                        exerciseLog.exercisedOn,
                        exerciseLog.memo
                )
                .from(exerciseLog)
                .where(
                        exerciseLog.playerId.eq(playerId),
                        exerciseLog.id.in(sourceIds)
                )
                .fetch();
        rows.forEach(row -> previews.put(
                new SourceKey(
                        LifeLogSourceType.EXERCISE,
                        row.get(exerciseLog.id)
                ),
                new LifeLogJournalResult.ExercisePreview(
                        row.get(exerciseLog.category).name(),
                        row.get(exerciseLog.metrics.durationMinutes),
                        row.get(exerciseLog.metrics.distanceKm),
                        row.get(exerciseLog.metrics.calories),
                        row.get(exerciseLog.exercisedOn),
                        row.get(exerciseLog.memo)
                )
        ));
    }

    private void loadMediaPreviews(
            Long playerId,
            Set<Long> sourceIds,
            Map<SourceKey, LifeLogJournalResult.Preview> previews
    ) {
        if (sourceIds.isEmpty()) {
            return;
        }
        List<Tuple> rows = queryFactory
                .select(
                        mediaLog.id,
                        mediaLog.category,
                        mediaLog.title.value,
                        mediaLog.progress.current,
                        mediaLog.progress.total,
                        mediaLog.status,
                        mediaLog.rating.score
                )
                .from(mediaLog)
                .where(
                        mediaLog.playerId.eq(playerId),
                        mediaLog.id.in(sourceIds)
                )
                .fetch();
        rows.forEach(row -> previews.put(
                new SourceKey(
                        LifeLogSourceType.MEDIA,
                        row.get(mediaLog.id)
                ),
                new LifeLogJournalResult.MediaPreview(
                        row.get(mediaLog.category).name(),
                        row.get(mediaLog.title.value),
                        row.get(mediaLog.progress.current),
                        row.get(mediaLog.progress.total),
                        row.get(mediaLog.status).name(),
                        row.get(mediaLog.rating.score)
                )
        ));
    }

    private Optional<LifeLogJournalResult.Source> loadCollectionSource(
            Long playerId,
            Long sourceId
    ) {
        CollectionLog log = queryFactory
                .selectFrom(collectionLog)
                .distinct()
                .leftJoin(collectionLog.tags.values).fetchJoin()
                .where(
                        collectionLog.playerId.eq(playerId),
                        collectionLog.id.eq(sourceId)
                )
                .fetchOne();
        return Optional.ofNullable(log).map(value ->
                new LifeLogJournalResult.CollectionSource(
                        value.getCategory().name(),
                        value.getTitle().value(),
                        value.getTitle().original(),
                        value.getQuantity().value(),
                        value.getConditionNote(),
                        value.getAcquiredFrom(),
                        value.getTags().values(),
                        value.getCreatedAt(),
                        value.getUpdatedAt()
                ));
    }

    private Optional<LifeLogJournalResult.Source> loadExerciseSource(
            Long playerId,
            Long sourceId
    ) {
        ExerciseLog log = queryFactory
                .selectFrom(exerciseLog)
                .where(
                        exerciseLog.playerId.eq(playerId),
                        exerciseLog.id.eq(sourceId)
                )
                .fetchOne();
        return Optional.ofNullable(log).map(value ->
                new LifeLogJournalResult.ExerciseSource(
                        value.getCategory().name(),
                        value.getMetrics().durationMinutes(),
                        value.getMetrics().distanceKm(),
                        value.getMetrics().calories(),
                        value.getExercisedOn(),
                        value.getMemo(),
                        value.getCreatedAt(),
                        value.getUpdatedAt()
                ));
    }

    private Optional<LifeLogJournalResult.Source> loadMediaSource(
            Long playerId,
            Long sourceId
    ) {
        MediaLog log = queryFactory
                .selectFrom(mediaLog)
                .distinct()
                .leftJoin(mediaLog.mediaTags.values).fetchJoin()
                .where(
                        mediaLog.playerId.eq(playerId),
                        mediaLog.id.eq(sourceId)
                )
                .fetchOne();
        return Optional.ofNullable(log).map(value ->
                new LifeLogJournalResult.MediaSource(
                        value.getCategory().name(),
                        value.getTitle().value(),
                        value.getTitle().original(),
                        value.getProgress().current(),
                        value.getProgress().total(),
                        value.getStatus().name(),
                        value.getRating().score(),
                        value.getMediaTags().values(),
                        value.getRewatchCount(),
                        value.getStartedOn(),
                        value.getFinishedOn(),
                        value.getCreatedAt(),
                        value.getUpdatedAt()
                ));
    }

    private Set<Long> sourceIds(
            List<CanonicalRecord> records,
            LifeLogSourceType sourceType
    ) {
        Set<Long> ids = new LinkedHashSet<>();
        records.stream()
                .filter(record -> record.sourceType() == sourceType)
                .map(CanonicalRecord::sourceId)
                .forEach(ids::add);
        return ids;
    }

    private BooleanExpression primaryRoleIdEq(Long primaryRoleId) {
        return primaryRoleId == null
                ? null
                : lifeLogRecord.primaryRoleId.eq(primaryRoleId);
    }

    private BooleanExpression subtypeEq(LifeLogSubtype subtype) {
        return subtype == null ? null : lifeLogRecord.subtype.eq(subtype);
    }
}
