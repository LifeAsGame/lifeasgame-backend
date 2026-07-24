package online.lifeasgame.quest.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestSignalReceipt")
class QuestSignalReceiptTest {

    @Nested
    @DisplayName("Signal 처리 사실을 생성할 때")
    class CreateReceipt {

        @Test
        @DisplayName("Identity와 Payload Snapshot을 보존한다")
        void snapshotsSignal() {
            String fingerprint = "a".repeat(64);

            QuestSignalReceipt receipt = QuestSignalReceipt.create(
                    QuestCode.COLLECTION_HUNTER_10.value(),
                    195L,
                    "source:collection:195",
                    "ADD_PROGRESS",
                    fingerprint,
                    Instant.parse("2026-07-24T01:00:00Z")
            );

            assertThat(receipt.getQuestCode())
                    .isEqualTo(QuestCode.COLLECTION_HUNTER_10.value());
            assertThat(receipt.getPlayerId()).isEqualTo(195L);
            assertThat(receipt.getCorrelationId())
                    .isEqualTo("source:collection:195");
            assertThat(receipt.getSignalType()).isEqualTo("ADD_PROGRESS");
            assertThat(receipt.hasFingerprint(fingerprint)).isTrue();
            assertThat(receipt.hasFingerprint("b".repeat(64))).isFalse();
        }
    }
}
