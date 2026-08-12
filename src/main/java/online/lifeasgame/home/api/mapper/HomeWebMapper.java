package online.lifeasgame.home.api.mapper;

import online.lifeasgame.home.api.response.HomeResponse;
import online.lifeasgame.home.application.result.HomeResult;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;

public final class HomeWebMapper {

    private HomeWebMapper() {
    }

    public static HomeResponse.Summary toSummary(HomeResult.Summary result) {
        return new HomeResponse.Summary(
                result.generatedAt(),
                result.recentJournal().stream()
                        .map(HomeWebMapper::toJournalEntry)
                        .toList(),
                new HomeResponse.Journey(
                        result.journey().currentQuests().stream()
                                .map(quest -> new HomeResponse.CurrentQuest(
                                        quest.acceptanceId(),
                                        quest.questCode(),
                                        quest.title(),
                                        quest.status(),
                                        quest.progressValue(),
                                        quest.targetValue(),
                                        quest.acceptedAt(),
                                        quest.goalReachedAt()
                                ))
                                .toList(),
                        result.journey().selectedRoutes().stream()
                                .map(route -> new HomeResponse.SelectedRoute(
                                        route.routeId(),
                                        route.routeCode(),
                                        route.title(),
                                        route.status(),
                                        route.currentStepId(),
                                        route.selectedAt(),
                                        route.completedAt()
                                ))
                                .toList()
                ),
                new HomeResponse.RoleActivity(
                        result.roleActivity30d().windowStart(),
                        result.roleActivity30d().windowEnd(),
                        result.roleActivity30d().totalRecords(),
                        result.roleActivity30d().assignedRecords(),
                        result.roleActivity30d().unassignedRecords(),
                        result.roleActivity30d().roles().stream()
                                .map(role -> new HomeResponse.RoleBucket(
                                        role.roleId(),
                                        role.roleName(),
                                        role.recordCount(),
                                        role.share()
                                ))
                                .toList()
                )
        );
    }

    private static HomeResponse.JournalEntry toJournalEntry(
            LifeLogJournalResult.Entry entry
    ) {
        return new HomeResponse.JournalEntry(
                entry.lifeLogId(),
                entry.sourceType(),
                entry.subtype(),
                entry.entryMode(),
                entry.primaryRoleId(),
                entry.roleEventId(),
                entry.recordedAt(),
                toPreview(entry.preview())
        );
    }

    private static HomeResponse.Preview toPreview(
            LifeLogJournalResult.Preview preview
    ) {
        return switch (preview) {
            case LifeLogJournalResult.CollectionPreview value ->
                    new HomeResponse.CollectionPreview(
                            value.category(),
                            value.title(),
                            value.quantity()
                    );
            case LifeLogJournalResult.ExercisePreview value ->
                    new HomeResponse.ExercisePreview(
                            value.category(),
                            value.durationMinutes(),
                            value.distanceKm(),
                            value.calories(),
                            value.exercisedOn(),
                            value.memo()
                    );
            case LifeLogJournalResult.MediaPreview value ->
                    new HomeResponse.MediaPreview(
                            value.category(),
                            value.title(),
                            value.currentEpisode(),
                            value.totalEpisode(),
                            value.status(),
                            value.rating()
                    );
        };
    }
}
