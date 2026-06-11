package online.lifeasgame.quest.application.event;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.quest.application.automation.QuestAutomationService;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.application.trigger.QuestTriggerRegistry;
import online.lifeasgame.quest.domain.event.QuestEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestEventBridge {

    private final QuestTriggerRegistry triggerRegistry;
    private final QuestAutomationService questAutomationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        if (event instanceof QuestEvent questEvent) {
            log.trace("Skipping quest event {} to avoid recursion", questEvent.type());
            return;
        }

        List<QuestSignal> signals = triggerRegistry.translate(event);
        if (signals.isEmpty()) {
            return;
        }

        log.debug("Translating {} into {} quest signals", event.getClass().getSimpleName(), signals.size());
        questAutomationService.processSignals(signals);
    }
}
