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
import org.springframework.test.util.ReflectionTestUtils;

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
    private static final Instant STORED_AT =
            Instant.parse("2026-07-24T01:00:00Z");
    private static final Instant RETRIED_AT =
            Instant.parse("2026-07-24T01:00:01Z");
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-24T00:00:00Z");

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

    @Test
    @DisplayName("같은 Manual Check는 stored occurredAt으로 재계산해 replay한다")
    void replaysManualCheckWithDifferentOccurredAt() {
        QuestSignal stored = manualSignal(
                STORED_AT,
                25,
                ACCEPTED_AT,
                null
        );
        QuestSignal retried = manualSignal(
                RETRIED_AT,
                25,
                ACCEPTED_AT,
                null
        );
        QuestSignalFingerprint realFingerprint =
                new QuestSignalFingerprint();
        QuestSignalReceiptReplayRecovery realRecovery =
                realRecovery(stored, realFingerprint);

        assertThat(realRecovery.recover(
                retried,
                realFingerprint.fingerprint(retried)
        )).contains(
                QuestSignalProcessingResult.replayed(RECEIPT_ID)
        );
    }

    @Test
    @DisplayName("Manual Check라도 progressValue가 다르면 conflict다")
    void rejectsDifferentManualCheckProgress() {
        assertManualConflict(
                manualSignal(STORED_AT, 25, ACCEPTED_AT, null),
                manualSignal(RETRIED_AT, 10, ACCEPTED_AT, null)
        );
    }

    @Test
    @DisplayName("Manual Check attempt acceptedAt이 다르면 conflict다")
    void rejectsDifferentManualCheckAttempt() {
        assertManualConflict(
                manualSignal(STORED_AT, 25, ACCEPTED_AT, null),
                manualSignal(
                        RETRIED_AT,
                        25,
                        ACCEPTED_AT.plusSeconds(1),
                        null
                )
        );
    }

    @Test
    @DisplayName("Manual Check periodKey가 다르면 conflict다")
    void rejectsDifferentManualCheckPeriodKey() {
        assertManualConflict(
                manualSignal(
                        STORED_AT,
                        25,
                        ACCEPTED_AT,
                        "2026-W30"
                ),
                manualSignal(
                        RETRIED_AT,
                        25,
                        ACCEPTED_AT,
                        "2026-W31"
                )
        );
    }

    @Test
    @DisplayName("manualCheck/source marker가 다르면 conflict다")
    void rejectsDifferentManualCheckMarkers() {
        QuestSignal stored = manualSignal(
                STORED_AT,
                25,
                ACCEPTED_AT,
                null
        );
        assertManualConflict(
                stored,
                manualSignal(
                        RETRIED_AT,
                        25,
                        ACCEPTED_AT,
                        null,
                        false,
                        "USER_CONFIRMATION"
                )
        );
        assertManualConflict(
                stored,
                manualSignal(
                        RETRIED_AT,
                        25,
                        ACCEPTED_AT,
                        null,
                        true,
                        "OTHER"
                )
        );
    }

    @Test
    @DisplayName("일반 EXISTING_ONLY의 occurredAt 차이는 계속 conflict다")
    void rejectsDifferentOccurredAtForGeneralExistingOnlySignal() {
        QuestSignal stored = existingOnlySignal(STORED_AT);
        QuestSignal retried = existingOnlySignal(RETRIED_AT);
        QuestSignalFingerprint realFingerprint =
                new QuestSignalFingerprint();
        QuestSignalReceiptReplayRecovery realRecovery =
                realRecovery(stored, realFingerprint);

        assertThatThrownBy(() -> realRecovery.recover(
                retried,
                realFingerprint.fingerprint(retried)
        )).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                QuestError
                                        .QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT
                        )
        );
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

    private void assertManualConflict(
            QuestSignal stored,
            QuestSignal retried
    ) {
        QuestSignalFingerprint realFingerprint =
                new QuestSignalFingerprint();
        QuestSignalReceiptReplayRecovery realRecovery =
                realRecovery(stored, realFingerprint);

        assertThatThrownBy(() -> realRecovery.recover(
                retried,
                realFingerprint.fingerprint(retried)
        )).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                QuestError
                                        .QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT
                        )
        );
    }

    private QuestSignalReceiptReplayRecovery realRecovery(
            QuestSignal stored,
            QuestSignalFingerprint realFingerprint
    ) {
        QuestSignalReceipt storedReceipt = QuestSignalReceipt.create(
                stored.questCode().value(),
                stored.playerId(),
                stored.correlationId(),
                stored.type().name(),
                realFingerprint.fingerprint(stored),
                stored.occurredAt()
        );
        ReflectionTestUtils.setField(
                storedReceipt,
                "id",
                RECEIPT_ID
        );
        given(receiptRepository.findByIdentity(
                stored.questCode().value(),
                stored.playerId(),
                stored.correlationId()
        )).willReturn(Optional.of(storedReceipt));
        return new QuestSignalReceiptReplayRecovery(
                receiptRepository,
                realFingerprint
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

    private QuestSignal manualSignal(
            Instant occurredAt,
            int progressValue,
            Instant attemptAcceptedAt,
            String periodKey
    ) {
        return manualSignal(
                occurredAt,
                progressValue,
                attemptAcceptedAt,
                periodKey,
                true,
                "USER_CONFIRMATION"
        );
    }

    private QuestSignal manualSignal(
            Instant occurredAt,
            int progressValue,
            Instant attemptAcceptedAt,
            String periodKey,
            boolean manualCheck,
            String source
    ) {
        return QuestSignal.setProgress(
                        QuestCode.Q_GROWTH_ONE_FOCUS,
                        195L,
                        progressValue
                )
                .occurredAt(occurredAt)
                .correlationId(
                        "manual-check:acceptance:1950:accepted-at:"
                                + ACCEPTED_AT.toEpochMilli()
                )
                .acceptancePolicy(
                        QuestSignalAcceptancePolicy.EXISTING_ONLY
                )
                .periodKey(periodKey)
                .acceptanceAttempt(1950L, attemptAcceptedAt)
                .attribute("acceptanceId", 1950L)
                .attribute("manualCheck", manualCheck)
                .attribute("source", source)
                .build();
    }

    private QuestSignal existingOnlySignal(Instant occurredAt) {
        return QuestSignal.addProgress(
                        QuestCode.Q_RECORD_THREE_TRACES,
                        195L,
                        1
                )
                .occurredAt(occurredAt)
                .correlationId("lifelog:195")
                .acceptancePolicy(
                        QuestSignalAcceptancePolicy.EXISTING_ONLY
                )
                .attribute("lifeLogId", 195L)
                .build();
    }
}
