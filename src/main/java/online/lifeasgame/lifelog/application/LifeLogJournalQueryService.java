package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.query.LifeLogJournalQuery;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LifeLogJournalQueryService {

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
        Map<LifeLogJournalQuery.SourceKey, LifeLogJournalResult.Preview>
                previews = journalQuery.loadPreviews(
                        playerId,
                        canonicalPage.content()
                );
        return new LifeLogJournalResult.Page(
                canonicalPage.content().stream()
                        .map(record -> LifeLogJournalResult.Entry.from(
                                record,
                                requirePreview(previews, record)
                        ))
                        .toList(),
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
