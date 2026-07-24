package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.error.QuestError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestSignalProcessingService")
class QuestSignalProcessingServiceTest {

    private static final String FINGERPRINT = "a".repeat(64);

    @Mock
    private QuestSignalFingerprint signalFingerprint;

    @Mock
    private QuestSignalProcessingAttempt processingAttempt;

    @Mock
    private QuestSignalReceiptReplayRecovery replayRecovery;

    private QuestSignalProcessingService service;

    @BeforeEach
    void setUp() {
        service = new QuestSignalProcessingService(
                signalFingerprint,
                processingAttempt,
                replayRecovery
        );
    }

    @Nested
    @DisplayName("최초 Signal을 처리할 때")
    class ApplySignal {

        @Test
        @DisplayName("Attempt의 APPLIED 결과를 반환한다")
        void returnsAppliedResult() {
            QuestSignal signal = signal(1);
            QuestSignalProcessingResult applied =
                    QuestSignalProcessingResult.applied(195L);
            given(signalFingerprint.fingerprint(signal))
                    .willReturn(FINGERPRINT);
            given(processingAttempt.process(signal, FINGERPRINT))
                    .willReturn(applied);

            QuestSignalProcessingResult result = service.process(signal);

            assertThat(result).isEqualTo(applied);
            verifyNoInteractions(replayRecovery);
        }
    }

    @Nested
    @DisplayName("Receipt unique 충돌이 발생할 때")
    class RecoverDuplicate {

        @Test
        @DisplayName("새 Transaction의 동일 Fingerprint Receipt를 REPLAYED로 반환한다")
        void returnsReplay() {
            QuestSignal signal = signal(1);
            DataIntegrityViolationException duplicate =
                    new DataIntegrityViolationException("duplicate");
            QuestSignalProcessingResult replayed =
                    QuestSignalProcessingResult.replayed(195L);
            given(signalFingerprint.fingerprint(signal))
                    .willReturn(FINGERPRINT);
            given(processingAttempt.process(signal, FINGERPRINT))
                    .willThrow(duplicate);
            given(replayRecovery.recover(signal, FINGERPRINT))
                    .willReturn(Optional.of(replayed));

            assertThat(service.process(signal)).isEqualTo(replayed);
        }

        @Test
        @DisplayName("동일 Identity의 다른 Payload는 안정된 DomainException을 전파한다")
        void rejectsPayloadConflict() {
            QuestSignal signal = signal(2);
            DataIntegrityViolationException duplicate =
                    new DataIntegrityViolationException("duplicate");
            DomainException conflict = new DomainException(
                    QuestError.QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT
            );
            given(signalFingerprint.fingerprint(signal))
                    .willReturn(FINGERPRINT);
            given(processingAttempt.process(signal, FINGERPRINT))
                    .willThrow(duplicate);
            given(replayRecovery.recover(signal, FINGERPRINT))
                    .willThrow(conflict);

            assertThatThrownBy(() -> service.process(signal))
                    .isSameAs(conflict);
        }

        @Test
        @DisplayName("Receipt가 없으면 다른 제약 실패로 보고 원래 예외를 전파한다")
        void rethrowsUnrelatedDataIntegrityViolation() {
            QuestSignal signal = signal(1);
            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("other constraint");
            given(signalFingerprint.fingerprint(signal))
                    .willReturn(FINGERPRINT);
            given(processingAttempt.process(signal, FINGERPRINT))
                    .willThrow(exception);
            given(replayRecovery.recover(signal, FINGERPRINT))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.process(signal))
                    .isSameAs(exception);
            verify(replayRecovery).recover(signal, FINGERPRINT);
        }
    }

    private QuestSignal signal(int delta) {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        195L,
                        delta
                )
                .occurredAt(Instant.parse("2026-07-24T01:00:00Z"))
                .correlationId("source:collection:195")
                .build();
    }
}
