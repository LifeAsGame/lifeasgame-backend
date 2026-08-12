package online.lifeasgame.lifelog.application.query;

import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LifeLogJournalQuery {

    CanonicalPage findPage(
            Long playerId,
            Long primaryRoleId,
            LifeLogSubtype subtype,
            int page,
            int size
    );

    Optional<CanonicalRecord> findOwned(Long playerId, Long lifeLogId);

    Map<SourceKey, LifeLogJournalResult.Preview> loadPreviews(
            Long playerId,
            List<CanonicalRecord> records
    );

    Optional<LifeLogJournalResult.Source> loadSource(
            Long playerId,
            CanonicalRecord record
    );

    record CanonicalPage(
            List<CanonicalRecord> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    record CanonicalRecord(
            Long lifeLogId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogSubtype subtype,
            LifeLogEntryMode entryMode,
            LifeLogReflectionScope reflectionScope,
            String periodKey,
            Long primaryRoleId,
            Long roleEventId,
            Instant occurredAt
    ) {
        public SourceKey sourceKey() {
            return new SourceKey(sourceType, sourceId);
        }
    }

    record SourceKey(LifeLogSourceType sourceType, Long sourceId) {
    }
}
