package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestSignalReceipt;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.QuestSignalReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestSignalReceiptReplayRecovery")
class QuestSignalReceiptReplayRecoveryTest {

    private static final String CURRENT_FINGERPRINT = "a".repeat(64);
    private static final String LEGACY_FINGERPRINT = "b".repeat(64);
    private static final Long RECEIPT_ID = 195L;

    @Mock
    private QuestSignalReceiptRepository receiptRepository;

    @Mock
    private QuestSignalFingerprint signalFingerprint;

    @Mock
    private QuestSignalReceipt receipt;

    private QuestSignalReceiptReplayRecovery recovery;

    @BeforeEach
    void setUp() {
        recovery = new QuestSignalReceiptReplayRecovery(
                receiptRepository,
                signalFingerprint
        );
    }

    @Test
    @DisplayName("Current fingerprint가 일치하면 legacy 계산 없이 REPLAYED다")
    void replaysCurrentFingerprintWithoutLegacyFallback() {
        QuestSignal signal = signal(
                QuestSignalAcceptancePolicy.EXISTING_ONLY,
                "2026-W30"
        );
        stubReceipt(signal);
        given(receipt.hasFingerprint(CURRENT_FINGERPRINT))
                .willReturn(true);
        given(receipt.getId()).willReturn(RECEIPT_ID);

        Optional<QuestSignalProcessingResult> result =
                recovery.recover(signal, CURRENT_FINGERPRINT);

        assertThat(result).contains(
                QuestSignalProcessingResult.replayed(RECEIPT_ID)
        );
        verifyNoInteractions(signalFingerprint);
    }

    @Test
    @DisplayName("AUTO_CREATE/null의 legacy fingerprint가 일치하면 REPLAYED다")
    void replaysLegacyAutoCreateNullPeriod() {
        QuestSignal signal = signal(
                QuestSignalAcceptancePolicy.AUTO_CREATE,
                null
        );
        stubReceipt(signal);
        given(receipt.hasFingerprint(anyString())).willAnswer(
                invocation -> LEGACY_FINGERPRINT.equals(
                        invocation.getArgument(0)
                )
        );
        given(signalFingerprint.legacyFingerprint(signal))
                .willReturn(LEGACY_FINGERPRINT);
        given(receipt.getId()).willReturn(RECEIPT_ID);

        Optional<QuestSignalProcessingResult> result =
                recovery.recover(signal, CURRENT_FINGERPRINT);

        assertThat(result).contains(
                QuestSignalProcessingResult.replayed(RECEIPT_ID)
        );
    }

    @Test
    @DisplayName("AUTO_CREATE/null이어도 legacy fingerprint가 다르면 conflict다")
    void rejectsDifferentLegacyPayload() {
        QuestSignal signal = signal(
                QuestSignalAcceptancePolicy.AUTO_CREATE,
                null
        );
        stubReceipt(signal);
        given(receipt.hasFingerprint(anyString())).willReturn(false);
        given(signalFingerprint.legacyFingerprint(signal))
                .willReturn(LEGACY_FINGERPRINT);

        assertPayloadConflict(signal);
    }

    @Test
    @DisplayName("EXISTING_ONLY는 저장값이 legacy hash여도 fallback하지 않는다")
    void rejectsLegacyExistingOnlySignal() {
        QuestSignal signal = signal(
                QuestSignalAcceptancePolicy.EXISTING_ONLY,
                null
        );
        stubLegacyReceipt(signal);

        assertPayloadConflict(signal);
        verifyNoInteractions(signalFingerprint);
    }

    @Test
    @DisplayName("non-null periodKey는 저장값이 legacy hash여도 fallback하지 않는다")
    void rejectsLegacyNonNullPeriodKeySignal() {
        QuestSignal signal = signal(
                QuestSignalAcceptancePolicy.AUTO_CREATE,
                "2026-W30"
        );
        stubLegacyReceipt(signal);

        assertPayloadConflict(signal);
        verifyNoInteractions(signalFingerprint);
    }

    private void stubReceipt(QuestSignal signal) {
        given(receiptRepository.findByIdentity(
                signal.questCode().value(),
                signal.playerId(),
                signal.correlationId()
        )).willReturn(Optional.of(receipt));
    }

    private void stubLegacyReceipt(QuestSignal signal) {
        stubReceipt(signal);
        given(receipt.hasFingerprint(anyString())).willAnswer(
                invocation -> LEGACY_FINGERPRINT.equals(
                        invocation.getArgument(0)
                )
        );
    }

    private void assertPayloadConflict(QuestSignal signal) {
        assertThatThrownBy(() ->
                recovery.recover(signal, CURRENT_FINGERPRINT)
        ).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                QuestError
                                        .QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT
                        )
        );
    }

    private QuestSignal signal(
            QuestSignalAcceptancePolicy acceptancePolicy,
            String periodKey
    ) {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        195L,
                        1
                )
                .occurredAt(Instant.parse("2026-07-24T01:00:00Z"))
                .correlationId("source:collection:195")
                .acceptancePolicy(acceptancePolicy)
                .periodKey(periodKey)
                .build();
    }
}
