package online.lifeasgame.economy.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.economy.domain.MarketplacePurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaMarketplacePurchaseReceiptRepository
        extends JpaRepository<MarketplacePurchaseReceipt, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO marketplace_purchase_receipts (
                buyer_player_id,
                idempotency_key,
                request_fingerprint,
                created_at,
                updated_at
            ) VALUES (
                :buyerPlayerId,
                :idempotencyKey,
                :requestFingerprint,
                :claimedAt,
                :claimedAt
            )
            ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
            """, nativeQuery = true)
    int claim(
            @Param("buyerPlayerId") Long buyerPlayerId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestFingerprint") String requestFingerprint,
            @Param("claimedAt") Instant claimedAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MarketplacePurchaseReceipt>
    findByBuyerPlayerIdAndIdempotencyKey(
            Long buyerPlayerId,
            String idempotencyKey
    );
}
