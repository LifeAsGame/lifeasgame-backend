package online.lifeasgame.quest.application.trigger;

import online.lifeasgame.lifelog.domain.event.ExerciseLogged;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.domain.QuestCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExerciseLoggedQuestTrigger implements QuestTrigger<ExerciseLogged> {

    @Override
    public Class<ExerciseLogged> eventType() {
        return ExerciseLogged.class;
    }

    @Override
    public List<QuestSignal> translate(ExerciseLogged event) {
        return List.of(
                QuestSignal.addProgress(QuestCode.EXERCISE_MINUTES_300, event.playerId(), event.durationMinutes())
                        .occurredAt(event.occurredAt())
                        .correlationId(QuestSignalCorrelation.sourceEvent(
                                "exercise",
                                event.playerId(),
                                event.exerciseLogId(),
                                event.occurredAt()
                        ))
                        .attribute("category", event.category())
                        .attribute("distanceKm", event.distanceKm())
                        .attribute("calories", event.calories())
                        .build()
        );
    }
}
