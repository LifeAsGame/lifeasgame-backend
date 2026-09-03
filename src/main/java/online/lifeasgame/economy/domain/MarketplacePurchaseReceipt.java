package online.lifeasgame.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Entity
@Table(
        name = "marketplace_purchase_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_marketplace_purchase_receipt_identity",
                columnNames = {"buyer_player_id", "idempotency_key"}
        ),
        indexes = @Index(
                name = "idx_marketplace_purchase_receipt_trade",
                columnList = "trade_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketplacePurchaseReceipt extends AbstractTime {

    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;
    public static final int REQUEST_FINGERPRINT_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_player_id", nullable = false)
    private Long buyerPlayerId;

    @Column(
            name = "idempotency_key",
            nullable = false,
            length = IDEMPOTENCY_KEY_MAX_LENGTH
    )
    private String idempotencyKey;

    @Column(
            name = "request_fingerprint",
            nullable = false,
            length = REQUEST_FINGERPRINT_LENGTH,
            columnDefinition = "CHAR(64)"
    )
    private String requestFingerprint;

    @Column(name = "trade_id")
    private Long tradeId;

    private MarketplacePurchaseReceipt(
            Long buyerPlayerId,
            String idempotencyKey,
            String requestFingerprint
    ) {
        if (buyerPlayerId == null || buyerPlayerId <= 0) {
            throw new IllegalArgumentException(
                    "buyerPlayerId must be positive"
            );
        }
        this.buyerPlayerId = buyerPlayerId;
        this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        this.requestFingerprint = requireFingerprint(requestFingerprint);
    }

    public static MarketplacePurchaseReceipt claim(
            Long buyerPlayerId,
            String idempotencyKey,
            String requestFingerprint
    ) {
        return new MarketplacePurchaseReceipt(
                buyerPlayerId,
                idempotencyKey,
                requestFingerprint
        );
    }

    public static String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw invalidIdempotencyKey();
        }
        String normalized = value.strip();
        if (normalized.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw invalidIdempotencyKey();
        }
        return normalized;
    }

    public static String fingerprint(
            Long listingId,
            String reservationToken
    ) {
        byte[] token = reservationToken == null
                ? new byte[0]
                : reservationToken.getBytes(StandardCharsets.UTF_8);
        ByteBuffer canonical = ByteBuffer.allocate(
                1 + Long.BYTES + 1 + Integer.BYTES + token.length
        );
        canonical.put((byte) (listingId == null ? 0 : 1));
        canonical.putLong(listingId == null ? 0L : listingId);
        canonical.put((byte) (reservationToken == null ? 0 : 1));
        canonical.putInt(token.length);
        canonical.put(token);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.array());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    public void assertRequestFingerprint(String candidate) {
        if (!requestFingerprint.equals(requireFingerprint(candidate))) {
            throw new DomainException(
                    EconomyError.IDEMPOTENCY_PAYLOAD_CONFLICT
            );
        }
    }

    public boolean isCompleted() {
        return tradeId != null;
    }

    public void complete(Long committedTradeId) {
        if (isCompleted()
                || committedTradeId == null
                || committedTradeId <= 0) {
            throw new IllegalStateException(
                    "Marketplace purchase receipt result is invalid"
            );
        }
        this.tradeId = committedTradeId;
    }

    public Long getTradeId() {
        if (!isCompleted()) {
            throw new IllegalStateException(
                    "Marketplace purchase receipt is incomplete"
            );
        }
        return tradeId;
    }

    private static String requireFingerprint(String value) {
        if (value == null
                || value.length() != REQUEST_FINGERPRINT_LENGTH
                || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException(
                    "Marketplace purchase fingerprint is invalid"
            );
        }
        return value;
    }

    private static DomainException invalidIdempotencyKey() {
        return new DomainException(EconomyError.INVALID_IDEMPOTENCY_KEY);
    }
}
