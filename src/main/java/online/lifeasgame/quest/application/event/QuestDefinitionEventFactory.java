package online.lifeasgame.quest.application.event;

import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class QuestDefinitionEventFactory {

    public QuestEvent created(Quest quest, Instant occurredAt) {
        return QuestEvent.snapshot(
                QuestEventType.QUEST_CREATED,
                quest,
                occurredAt,
                "quest:" + quest.getCode()
        );
    }

    public QuestEvent updated(Quest quest, Instant occurredAt) {
        return QuestEvent.snapshot(
                QuestEventType.QUEST_UPDATED,
                quest,
                occurredAt,
                "quest:" + quest.getCode() + ":updated"
        );
    }
}
