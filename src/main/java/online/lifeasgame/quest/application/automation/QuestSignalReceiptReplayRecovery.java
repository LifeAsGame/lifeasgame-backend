package online.lifeasgame.quest.application.automation;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.QuestSignalReceipt;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.QuestSignalReceiptRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QuestSignalReceiptReplayRecovery {

    private final QuestSignalReceiptRepository receiptRepository;
    private final QuestSignalFingerprint signalFingerprint;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<QuestSignalProcessingResult> recover(
            QuestSignal signal,
            String payloadFingerprint
    ) {
        return receiptRepository.findByIdentity(
                        signal.questCode().value(),
                        signal.playerId(),
                        signal.correlationId()
                )
                .map(receipt -> replay(
                        receipt,
                        signal,
                        payloadFingerprint
                ));
    }

    private QuestSignalProcessingResult replay(
            QuestSignalReceipt receipt,
            QuestSignal signal,
            String payloadFingerprint
    ) {
        if (receipt.hasFingerprint(payloadFingerprint)) {
            return QuestSignalProcessingResult.replayed(receipt.getId());
        }
        if (signal.acceptancePolicy()
                == QuestSignalAcceptancePolicy.AUTO_CREATE
                && signal.periodKey() == null
                && receipt.hasFingerprint(
                        signalFingerprint.legacyFingerprint(signal)
                )) {
            return QuestSignalProcessingResult.replayed(receipt.getId());
        }
        throw new DomainException(
                QuestError.QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT
        );
    }
}
