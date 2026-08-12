package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.internal.LifeLogActivityReadApi;
import online.lifeasgame.lifelog.application.query.LifeLogJournalQuery;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LifeLogJournalQueryService implements LifeLogActivityReadApi {

    private final LifeLogJournalQuery journalQuery;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public LifeLogJournalResult.Page list(
            Long primaryRoleId,
            String subtype,
            int page,
            int size
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        LifeLogJournalQuery.CanonicalPage canonicalPage =
                journalQuery.findPage(
                        playerId,
                        primaryRoleId,
                        subtype == null ? null : LifeLogSubtype.parse(subtype),
                        page,
                        size
                );
        return new LifeLogJournalResult.Page(
                enrich(playerId, canonicalPage.content()),
                canonicalPage.page(),
                canonicalPage.size(),
                canonicalPage.totalElements(),
                canonicalPage.totalPages()
        );
    }

    public LifeLogJournalResult.Detail detail(Long lifeLogId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        LifeLogJournalQuery.CanonicalRecord record = journalQuery
                .findOwned(playerId, lifeLogId)
                .orElseThrow(() -> new DomainException(
                        LifeLogError.LIFE_LOG_NOT_FOUND
                ));
        LifeLogJournalResult.Source source = journalQuery
                .loadSource(playerId, record)
                .orElseThrow(LifeLogJournalQueryService::sourceMissing);
        return LifeLogJournalResult.Detail.from(record, source);
    }

    @Override
    public List<LifeLogJournalResult.Entry> recentJournal(
            Long playerId,
            int limit
    ) {
        return enrich(playerId, journalQuery.findRecent(playerId, limit));
    }

    @Override
    public RoleActivity roleActivity(
            Long playerId,
            Instant windowStart,
            Instant windowEnd
    ) {
        List<LifeLogJournalQuery.RoleCount> counts =
                journalQuery.countByPrimaryRole(
                        playerId,
                        windowStart,
                        windowEnd
                );
        long assigned = counts.stream()
                .filter(count -> count.roleId() != null)
                .mapToLong(LifeLogJournalQuery.RoleCount::recordCount)
                .sum();
        long unassigned = counts.stream()
                .filter(count -> count.roleId() == null)
                .mapToLong(LifeLogJournalQuery.RoleCount::recordCount)
                .sum();
        return new RoleActivity(
                assigned + unassigned,
                assigned,
                unassigned,
                counts.stream()
                        .filter(count -> count.roleId() != null)
                        .map(count -> new LifeLogActivityReadApi.RoleCount(
                                count.roleId(),
                                count.recordCount()
                        ))
                        .toList()
        );
    }

    private List<LifeLogJournalResult.Entry> enrich(
            Long playerId,
            List<LifeLogJournalQuery.CanonicalRecord> records
    ) {
        Map<LifeLogJournalQuery.SourceKey, LifeLogJournalResult.Preview>
                previews = journalQuery.loadPreviews(playerId, records);
        return records.stream()
                .map(record -> LifeLogJournalResult.Entry.from(
                        record,
                        requirePreview(previews, record)
                ))
                .toList();
    }

    private LifeLogJournalResult.Preview requirePreview(
            Map<LifeLogJournalQuery.SourceKey, LifeLogJournalResult.Preview>
                    previews,
            LifeLogJournalQuery.CanonicalRecord record
    ) {
        LifeLogJournalResult.Preview preview = previews.get(
                record.sourceKey()
        );
        if (preview == null) {
            throw sourceMissing();
        }
        return preview;
    }

    private static DomainException sourceMissing() {
        return new DomainException(LifeLogError.LIFE_LOG_SOURCE_UNAVAILABLE);
    }
}
