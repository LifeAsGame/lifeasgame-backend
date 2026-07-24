package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.character.domain.event.PlayerRegistered;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlayerRegisteredQuestTrigger implements QuestTrigger<PlayerRegistered> {

    @Override
    public Class<PlayerRegistered> eventType() {
        return PlayerRegistered.class;
    }

    @Override
    public List<QuestSignal> translate(PlayerRegistered event) {
        String correlation = QuestSignalCorrelation.sourceEvent(
                "registered",
                event.playerId(),
                event.playerId(),
                event.occurredAt()
        );
        return List.of(
                QuestSignal.addProgress(QuestCode.PLAYER_WELCOME, event.playerId(), 1)
                        .occurredAt(event.occurredAt())
                        .correlationId(correlation)
                        .attribute("event", "PLAYER_REGISTERED")
                        .build(),
                QuestSignal.setProgress(QuestCode.PLAYER_LEVEL_TRACK, event.playerId(), 0)
                        .occurredAt(event.occurredAt())
                        .correlationId(correlation)
                        .attribute("event", "PLAYER_REGISTERED")
                        .build()
        );
    }
}
