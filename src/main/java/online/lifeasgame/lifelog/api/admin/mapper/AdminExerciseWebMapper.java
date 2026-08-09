package online.lifeasgame.lifelog.api.admin.mapper;

import online.lifeasgame.lifelog.api.admin.request.AdminExerciseRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminExerciseResponse;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.query.ExerciseQuery;
import online.lifeasgame.lifelog.application.result.ExerciseResult;

import java.time.LocalDate;
import java.util.List;

public final class AdminExerciseWebMapper {

    private AdminExerciseWebMapper() {
    }

    public static ExerciseQuery.Search toSearchQuery(
            String category,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        return new ExerciseQuery.Search(category, from, to, page, size);
    }

    public static List<AdminExerciseResponse.Info> toInfos(List<ExerciseResult.Info> results) {
        return results.stream().map(AdminExerciseWebMapper::toInfo).toList();
    }

    public static ExerciseCommand.Create toCreateCommand(AdminExerciseRequest.Create request) {
        return new ExerciseCommand.Create(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo()
        );
    }

    public static AdminExerciseResponse.Created toCreated(ExerciseResult.Created result) {
        return new AdminExerciseResponse.Created(result.id());
    }

    public static ExerciseCommand.Update toUpdateCommand(AdminExerciseRequest.Update request) {
        return new ExerciseCommand.Update(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo()
        );
    }

    public static AdminExerciseResponse.Info toInfo(ExerciseResult.Info result) {
        return new AdminExerciseResponse.Info(
                result.id(),
                result.playerId(),
                result.category(),
                result.durationMinutes(),
                result.distanceKm(),
                result.calories(),
                result.exercisedOn(),
                result.memo(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static AdminExerciseResponse.Deleted toDeleted(ExerciseResult.Deleted result) {
        return new AdminExerciseResponse.Deleted(result.exerciseId());
    }
}
