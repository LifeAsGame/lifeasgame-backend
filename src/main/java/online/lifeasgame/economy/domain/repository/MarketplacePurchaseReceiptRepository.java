package online.lifeasgame.economy.domain.repository;

import online.lifeasgame.economy.domain.MarketplacePurchaseReceipt;

import java.time.Instant;
import java.util.Optional;

public interface MarketplacePurchaseReceiptRepository {

    void claim(
            Long buyerPlayerId,
            String idempotencyKey,
            String requestFingerprint,
            Instant claimedAt
    );

    Optional<MarketplacePurchaseReceipt> findByIdentityForUpdate(
            Long buyerPlayerId,
            String idempotencyKey
    );

    MarketplacePurchaseReceipt saveAndFlush(
            MarketplacePurchaseReceipt receipt
    );
}
