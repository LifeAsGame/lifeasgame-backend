package online.lifeasgame.lifelog.quick.domain.repository;

import online.lifeasgame.lifelog.quick.domain.QuickRecordRequestReceipt;

import java.time.Instant;
import java.util.Optional;

public interface QuickRecordRequestReceiptRepository {

    void reserve(
            Long playerId,
            String idempotencyKey,
            String requestHash,
            Instant reservedAt
    );

    Optional<QuickRecordRequestReceipt> findByIdentityForUpdate(
            Long playerId,
            String idempotencyKey
    );

    QuickRecordRequestReceipt saveAndFlush(
            QuickRecordRequestReceipt receipt
    );
}
