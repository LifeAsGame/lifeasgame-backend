package online.lifeasgame.lifelog.quick.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.quick.domain.QuickRecordRequestReceipt;
import online.lifeasgame.lifelog.quick.domain.repository.QuickRecordRequestReceiptRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuickRecordRequestReceiptRepositoryAdapter
        implements QuickRecordRequestReceiptRepository {

    private final JpaQuickRecordRequestReceiptRepository jpaRepository;

    @Override
    public void reserve(
            Long playerId,
            String idempotencyKey,
            String requestHash,
            Instant reservedAt
    ) {
        jpaRepository.reserve(
                playerId,
                idempotencyKey,
                requestHash,
                reservedAt
        );
    }

    @Override
    public Optional<QuickRecordRequestReceipt> findByIdentityForUpdate(
            Long playerId,
            String idempotencyKey
    ) {
        return jpaRepository.findByPlayerIdAndIdempotencyKey(
                playerId,
                idempotencyKey
        );
    }

    @Override
    public QuickRecordRequestReceipt saveAndFlush(
            QuickRecordRequestReceipt receipt
    ) {
        return jpaRepository.saveAndFlush(receipt);
    }
}
