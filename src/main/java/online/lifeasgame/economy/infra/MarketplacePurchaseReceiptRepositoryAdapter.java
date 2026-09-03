package online.lifeasgame.economy.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.MarketplacePurchaseReceipt;
import online.lifeasgame.economy.domain.repository.MarketplacePurchaseReceiptRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MarketplacePurchaseReceiptRepositoryAdapter
        implements MarketplacePurchaseReceiptRepository {

    private final JpaMarketplacePurchaseReceiptRepository jpaRepository;

    @Override
    public void claim(
            Long buyerPlayerId,
            String idempotencyKey,
            String requestFingerprint,
            Instant claimedAt
    ) {
        jpaRepository.claim(
                buyerPlayerId,
                idempotencyKey,
                requestFingerprint,
                claimedAt
        );
    }

    @Override
    public Optional<MarketplacePurchaseReceipt>
    findByIdentityForUpdate(
            Long buyerPlayerId,
            String idempotencyKey
    ) {
        return jpaRepository.findByBuyerPlayerIdAndIdempotencyKey(
                buyerPlayerId,
                idempotencyKey
        );
    }

    @Override
    public MarketplacePurchaseReceipt saveAndFlush(
            MarketplacePurchaseReceipt receipt
    ) {
        return jpaRepository.saveAndFlush(receipt);
    }
}
