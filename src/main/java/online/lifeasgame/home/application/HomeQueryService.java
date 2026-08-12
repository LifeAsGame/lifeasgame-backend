package online.lifeasgame.home.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.AchievementProgressReadApi;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.home.application.result.HomeResult;
import online.lifeasgame.lifelog.application.internal.LifeLogActivityReadApi;
import online.lifeasgame.quest.application.internal.QuestProgressReadApi;
import online.lifeasgame.role.application.internal.RoleDisplayReadApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeQueryService {

    static final int RECENT_JOURNAL_LIMIT = 5;
    static final int RECENT_ACHIEVEMENT_LIMIT = 5;
    static final int CURRENT_QUEST_LIMIT = 10;
    static final int SELECTED_ROUTE_LIMIT = 10;
    private static final Duration ROLE_ACTIVITY_WINDOW = Duration.ofDays(30);

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final Clock clock;
    private final AchievementProgressReadApi achievementProgressReadApi;
    private final LifeLogActivityReadApi lifeLogActivityReadApi;
    private final QuestProgressReadApi questProgressReadApi;
    private final RoleDisplayReadApi roleDisplayReadApi;

    public HomeResult.Summary home() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Instant generatedAt = clock.instant();
        Instant windowStart = generatedAt.minus(ROLE_ACTIVITY_WINDOW);
        LifeLogActivityReadApi.RoleActivity activity =
                lifeLogActivityReadApi.roleActivity(
                        playerId,
                        windowStart,
                        generatedAt
                );
        Map<Long, String> roleNames = roleDisplayReadApi.findNames(
                playerId,
                activity.roles().stream()
                        .map(LifeLogActivityReadApi.RoleCount::roleId)
                        .toList()
        );

        return new HomeResult.Summary(
                generatedAt,
                lifeLogActivityReadApi.recentJournal(
                        playerId,
                        RECENT_JOURNAL_LIMIT
                ),
                achievementProgressReadApi.recentAchievements(
                        playerId,
                        RECENT_ACHIEVEMENT_LIMIT
                ),
                new HomeResult.Journey(
                        questProgressReadApi.currentQuests(
                                playerId,
                                CURRENT_QUEST_LIMIT
                        ),
                        questProgressReadApi.selectedRoutes(
                                playerId,
                                SELECTED_ROUTE_LIMIT
                        )
                ),
                roleActivity(
                        windowStart,
                        generatedAt,
                        activity,
                        roleNames
                )
        );
    }

    private HomeResult.RoleActivity roleActivity(
            Instant windowStart,
            Instant windowEnd,
            LifeLogActivityReadApi.RoleActivity activity,
            Map<Long, String> roleNames
    ) {
        List<HomeResult.RoleBucket> roles = activity.assignedRecords() == 0
                ? List.of()
                : activity.roles().stream()
                        .map(role -> new HomeResult.RoleBucket(
                                role.roleId(),
                                roleNames.get(role.roleId()),
                                role.recordCount(),
                                (double) role.recordCount()
                                        / activity.assignedRecords()
                        ))
                        .toList();
        return new HomeResult.RoleActivity(
                windowStart,
                windowEnd,
                activity.totalRecords(),
                activity.assignedRecords(),
                activity.unassignedRecords(),
                roles
        );
    }
}
