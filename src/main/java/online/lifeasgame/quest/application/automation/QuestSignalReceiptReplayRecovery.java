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
                .map(receipt -> replay(receipt, payloadFingerprint));
    }

    private QuestSignalProcessingResult replay(
            QuestSignalReceipt receipt,
            String payloadFingerprint
    ) {
        if (!receipt.hasFingerprint(payloadFingerprint)) {
            throw new DomainException(
                    QuestError.QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT
            );
        }
        return QuestSignalProcessingResult.replayed(receipt.getId());
    }
}
