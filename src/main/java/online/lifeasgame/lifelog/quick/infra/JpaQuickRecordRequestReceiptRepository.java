package online.lifeasgame.lifelog.quick.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.lifelog.quick.domain.QuickRecordRequestReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaQuickRecordRequestReceiptRepository
        extends JpaRepository<QuickRecordRequestReceipt, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO quick_record_request_receipts (
                player_id,
                idempotency_key,
                request_hash,
                created_at,
                updated_at
            ) VALUES (
                :playerId,
                :idempotencyKey,
                :requestHash,
                :reservedAt,
                :reservedAt
            )
            ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
            """, nativeQuery = true)
    int reserve(
            @Param("playerId") Long playerId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash,
            @Param("reservedAt") Instant reservedAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QuickRecordRequestReceipt>
    findByPlayerIdAndIdempotencyKey(
            Long playerId,
            String idempotencyKey
    );
}
