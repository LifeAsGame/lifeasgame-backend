package online.lifeasgame.quest.application.automation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestAutomationService {

    private final QuestSignalProcessingService processingService;

    public List<QuestSignalProcessingResult> processSignals(
            Collection<QuestSignal> signals
    ) {
        if (signals == null || signals.isEmpty()) {
            return List.of();
        }
        return signals.stream()
                .map(processingService::process)
                .toList();
    }
}
