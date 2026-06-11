package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.quest.application.automation.QuestSignal;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class QuestTriggerRegistry {

    private final Map<Class<?>, List<QuestTrigger<?>>> triggersByEvent;

    public QuestTriggerRegistry(List<QuestTrigger<?>> triggers) {
        Map<Class<?>, List<QuestTrigger<?>>> tmp = new LinkedHashMap<>();
        for (QuestTrigger<?> trigger : triggers) {
            tmp.computeIfAbsent(trigger.eventType(), ignored -> new ArrayList<>()).add(trigger);
        }
        this.triggersByEvent = tmp.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
    }

    public List<QuestSignal> translate(DomainEvent event) {
        if (event == null) {
            return List.of();
        }
        List<QuestTrigger<?>> triggers = triggersByEvent.get(event.getClass());
        if (triggers == null || triggers.isEmpty()) {
            return List.of();
        }
        return triggers.stream()
                .flatMap(trigger -> translateWith(trigger, event).stream())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<QuestSignal> translateWith(QuestTrigger<?> trigger, DomainEvent event) {
        Collection<QuestSignal> translated = ((QuestTrigger<DomainEvent>) trigger).translate(event);
        if (translated == null || translated.isEmpty()) {
            return List.of();
        }
        return List.copyOf(translated);
    }
}
