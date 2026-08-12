package online.lifeasgame.home.application.result;

import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;
import online.lifeasgame.quest.application.internal.QuestProgressReadApi;

import java.time.Instant;
import java.util.List;

public final class HomeResult {

    private HomeResult() {
    }

    public record Summary(
            Instant generatedAt,
            List<LifeLogJournalResult.Entry> recentJournal,
            Journey journey,
            RoleActivity roleActivity30d
    ) {
        public Summary {
            recentJournal = List.copyOf(recentJournal);
        }
    }

    public record Journey(
            List<QuestProgressReadApi.CurrentQuest> currentQuests,
            List<QuestProgressReadApi.SelectedRoute> selectedRoutes
    ) {
        public Journey {
            currentQuests = List.copyOf(currentQuests);
            selectedRoutes = List.copyOf(selectedRoutes);
        }
    }

    public record RoleActivity(
            Instant windowStart,
            Instant windowEnd,
            long totalRecords,
            long assignedRecords,
            long unassignedRecords,
            List<RoleBucket> roles
    ) {
        public RoleActivity {
            roles = List.copyOf(roles);
        }
    }

    public record RoleBucket(
            Long roleId,
            String roleName,
            long recordCount,
            double share
    ) {
    }
}
