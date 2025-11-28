package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.inventory.domain.event.InventoryItemAdded;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryItemAddedQuestTrigger implements QuestTrigger<InventoryItemAdded> {

    @Override
    public Class<InventoryItemAdded> eventType() {
        return InventoryItemAdded.class;
    }

    @Override
    public List<QuestSignal> translate(InventoryItemAdded event) {
        return List.of(
                QuestSignal.addProgress(QuestCode.INVENTORY_COLLECTOR_100, event.playerId(), event.quantity())
                        .occurredAt(event.occurredAt())
                        .correlationId("player:" + event.playerId() + ":inventory:" + event.itemId())
                        .attribute("rarity", event.rarity())
                        .attribute("stackable", event.stackable())
                        .attribute("bound", event.bound())
                        .build()
        );
    }
}
