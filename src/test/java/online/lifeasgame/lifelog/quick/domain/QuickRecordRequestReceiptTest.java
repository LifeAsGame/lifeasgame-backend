package online.lifeasgame.lifelog.quick.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuickRecordRequestReceipt")
class QuickRecordRequestReceiptTest {

    private static final String HASH = "a".repeat(64);
    private static final Instant RECORDED_AT =
            Instant.parse("2026-07-24T13:00:00Z");

    @Test
    @DisplayName("동일 hash replay는 실제 Source snapshot을 반환한다")
    void replaysStoredSource() {
        QuickRecordRequestReceipt receipt =
                QuickRecordRequestReceipt.reserve(
                        201L,
                        " key-201 ",
                        HASH
                );
        receipt.complete(
                LifeLogType.MEDIA,
                31L,
                RECORDED_AT
        );

        QuickRecordRequestReceipt.StoredResult replay =
                receipt.replay(HASH);

        assertThat(receipt.getIdempotencyKey()).isEqualTo("key-201");
        assertThat(replay.sourceType()).isEqualTo(LifeLogType.MEDIA);
        assertThat(replay.sourceId()).isEqualTo(31L);
        assertThat(replay.recordedAt()).isEqualTo(RECORDED_AT);
    }

    @Test
    @DisplayName("다른 hash는 저장 결과를 노출하지 않고 conflict로 거부한다")
    void rejectsDifferentPayloadHash() {
        QuickRecordRequestReceipt receipt =
                QuickRecordRequestReceipt.reserve(
                        201L,
                        "key-201",
                        HASH
                );
        receipt.complete(
                LifeLogType.EXERCISE,
                31L,
                RECORDED_AT
        );

        assertThatThrownBy(() -> receipt.replay("b".repeat(64)))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(
                                            QuickRecordError
                                                    .IDEMPOTENCY_KEY_PAYLOAD_CONFLICT
                                    );
                            assertThat(exception.detail()).isNull();
                        }
                );
    }

    @Test
    @DisplayName("Idempotency-Key 필수와 DB column 길이를 검증한다")
    void validatesIdempotencyKey() {
        assertThatThrownBy(() ->
                QuickRecordRequestReceipt.normalizeIdempotencyKey(" ")
        ).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                QuickRecordError.IDEMPOTENCY_KEY_REQUIRED
                        )
        );
        assertThatThrownBy(() ->
                QuickRecordRequestReceipt.normalizeIdempotencyKey(
                        "a".repeat(
                                QuickRecordRequestReceipt
                                        .IDEMPOTENCY_KEY_MAX_LENGTH + 1
                        )
                )
        ).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(QuickRecordError.INVALID_REQUEST)
        );
    }
}
