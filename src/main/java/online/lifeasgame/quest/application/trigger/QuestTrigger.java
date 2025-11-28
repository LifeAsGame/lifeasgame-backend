package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.quest.application.automation.QuestSignal;

import java.util.Collection;

public interface QuestTrigger<T extends DomainEvent> {

    Class<T> eventType();

    Collection<QuestSignal> translate(T event);
}
