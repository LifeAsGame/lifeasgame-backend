package online.lifeasgame.quest.application.automation;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestSignalProcessingService {

    private final QuestSignalFingerprint signalFingerprint;
    private final QuestSignalProcessingAttempt processingAttempt;
    private final QuestSignalReceiptReplayRecovery replayRecovery;

    public QuestSignalProcessingResult process(QuestSignal signal) {
        String payloadFingerprint = signalFingerprint.fingerprint(signal);
        try {
            return processingAttempt.process(signal, payloadFingerprint);
        } catch (DataIntegrityViolationException exception) {
            return replayRecovery.recover(signal, payloadFingerprint)
                    .orElseThrow(() -> exception);
        }
    }
}
