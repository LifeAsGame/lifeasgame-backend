package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.application.internal.LifeLogEvidenceReadApi;
import online.lifeasgame.lifelog.application.query.LifeLogJournalQuery;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LifeLogEvidenceLookup implements LifeLogEvidenceReadApi {
    private final LifeLogJournalQuery journal;

    @Override
    public Evidence requireContentfulOwned(Long playerId, Long lifeLogId) {
        var record = journal.findOwned(playerId, lifeLogId)
                .orElseThrow(() -> new DomainException(LifeLogError.LIFE_LOG_NOT_FOUND));
        var source = journal.loadSource(playerId, record)
                .orElseThrow(() -> new DomainException(LifeLogError.LIFE_LOG_NOT_FOUND));
        boolean contentful = switch (source) {
            case LifeLogJournalResult.CollectionSource item -> hasText(item.title());
            case LifeLogJournalResult.MediaSource media -> hasText(media.title());
            case LifeLogJournalResult.ExerciseSource exercise -> hasText(exercise.memo());
        };
        if (!contentful) throw new DomainException(LifeLogError.INVALID_STATE);
        boolean project = source instanceof LifeLogJournalResult.CollectionSource item
                && "PROJECT".equals(item.category());
        return new Evidence(lifeLogId, record.primaryRoleId(), project);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
