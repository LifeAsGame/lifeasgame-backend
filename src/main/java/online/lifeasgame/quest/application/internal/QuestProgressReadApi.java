package online.lifeasgame.quest.application.internal;

import java.time.Instant;
import java.util.List;

public interface QuestProgressReadApi {

    List<CurrentQuest> currentQuests(Long playerId, int limit);

    List<SelectedRoute> selectedRoutes(Long playerId, int limit);

    record CurrentQuest(
            Long acceptanceId,
            String questCode,
            String title,
            String status,
            int progressValue,
            int targetValue,
            Instant acceptedAt,
            Instant goalReachedAt
    ) {
    }

    record SelectedRoute(
            Long routeId,
            String routeCode,
            String title,
            String status,
            Long currentStepId,
            Instant selectedAt,
            Instant completedAt
    ) {
    }
}
