package online.lifeasgame.lifelog.quick.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.application.CollectionLogService;
import online.lifeasgame.lifelog.application.ExerciseLogService;
import online.lifeasgame.lifelog.application.MediaLogService;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.lifelog.application.result.MediaLogResult;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.quick.domain.QuickRecordRequestReceipt;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;
import online.lifeasgame.lifelog.quick.domain.repository.QuickRecordRequestReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class QuickRecordService {

    private final CollectionLogService collectionLogService;
    private final ExerciseLogService exerciseLogService;
    private final MediaLogService mediaLogService;
    private final QuickRecordRequestReceiptRepository receiptRepository;
    private final QuickRecordRequestHasher requestHasher;
    private final Clock clock;

    @Transactional
    public QuickRecordResult.Recorded record(
            Long playerId,
            String idempotencyKey,
            QuickRecordCommand.Create command
    ) {
        if (playerId == null || playerId <= 0 || command == null) {
            throw invalid();
        }
        String normalizedKey =
                QuickRecordRequestReceipt.normalizeIdempotencyKey(
                        idempotencyKey
                );
        QuickRecordCommand.Selected selected = command.selected();
        String requestHash = requestHasher.hash(selected);
        Instant reservedAt = clock.instant();

        receiptRepository.reserve(
                playerId,
                normalizedKey,
                requestHash,
                reservedAt
        );
        QuickRecordRequestReceipt receipt = receiptRepository
                .findByIdentityForUpdate(playerId, normalizedKey)
                .orElseThrow(QuickRecordService::invalid);
        receipt.assertRequestHash(requestHash);

        if (receipt.isCompleted()) {
            return QuickRecordResult.Recorded.from(
                    receipt.replay(requestHash),
                    true
            );
        }

        SourceSnapshot source = createSource(playerId, selected);
        receipt.complete(
                source.sourceType(),
                source.sourceId(),
                source.recordedAt()
        );
        receiptRepository.saveAndFlush(receipt);

        return QuickRecordResult.Recorded.from(
                receipt.storedResult(),
                false
        );
    }

    private SourceSnapshot createSource(
            Long playerId,
            QuickRecordCommand.Selected selected
    ) {
        return switch (selected.type()) {
            case COLLECTION -> {
                CollectionResult.Created created =
                        collectionLogService.create(
                                playerId,
                                selected.collection()
                        );
                yield new SourceSnapshot(
                        LifeLogType.COLLECTION,
                        created.id(),
                        created.recordedAt()
                );
            }
            case EXERCISE -> {
                ExerciseResult.Created created =
                        exerciseLogService.create(
                                playerId,
                                selected.exercise()
                        );
                yield new SourceSnapshot(
                        LifeLogType.EXERCISE,
                        created.id(),
                        created.recordedAt()
                );
            }
            case MEDIA -> {
                MediaLogResult.Created created =
                        mediaLogService.create(
                                playerId,
                                selected.media()
                        );
                yield new SourceSnapshot(
                        LifeLogType.MEDIA,
                        created.id(),
                        created.recordedAt()
                );
            }
        };
    }

    private static DomainException invalid() {
        return new DomainException(QuickRecordError.INVALID_REQUEST);
    }

    private record SourceSnapshot(
            LifeLogType sourceType,
            Long sourceId,
            Instant recordedAt
    ) {
    }
}
