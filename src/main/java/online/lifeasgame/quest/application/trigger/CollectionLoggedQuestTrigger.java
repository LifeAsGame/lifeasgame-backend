package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.lifelog.domain.event.CollectionLogged;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CollectionLoggedQuestTrigger implements QuestTrigger<CollectionLogged> {

    @Override
    public Class<CollectionLogged> eventType() {
        return CollectionLogged.class;
    }

    @Override
    public List<QuestSignal> translate(CollectionLogged event) {
        return List.of(
                QuestSignal.addProgress(QuestCode.COLLECTION_HUNTER_10, event.playerId(), event.quantity())
                        .occurredAt(event.occurredAt())
                        .correlationId(QuestSignalCorrelation.sourceEvent(
                                "collection",
                                event.playerId(),
                                event.collectionLogId(),
                                event.occurredAt()
                        ))
                        .attribute("category", event.category())
                        .attribute("quantity", event.quantity())
                        .build()
        );
    }
}
