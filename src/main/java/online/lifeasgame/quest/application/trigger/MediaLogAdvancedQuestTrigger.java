package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.lifelog.domain.event.MediaLogAdvanced;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MediaLogAdvancedQuestTrigger implements QuestTrigger<MediaLogAdvanced> {

    @Override
    public Class<MediaLogAdvanced> eventType() {
        return MediaLogAdvanced.class;
    }

    @Override
    public List<QuestSignal> translate(MediaLogAdvanced event) {
        return List.of(
                QuestSignal.addProgress(QuestCode.MEDIA_BINGE_5, event.playerId(), event.advancedBy())
                        .occurredAt(event.occurredAt())
                        .correlationId(QuestSignalCorrelation.sourceEvent(
                                "media-advanced",
                                event.playerId(),
                                event.mediaLogId(),
                                event.occurredAt()
                        ))
                        .attribute("currentStep", event.currentStep())
                        .attribute("totalEpisodes", event.totalEpisodes())
                        .build()
        );
    }
}
