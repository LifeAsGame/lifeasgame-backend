package online.lifeasgame.lifelog.api.admin.mapper;

import online.lifeasgame.lifelog.api.admin.request.AdminExerciseRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminExerciseResponse;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.result.ExerciseResult;

import java.time.LocalDate;
import java.util.List;

public final class AdminExerciseWebMapper {
    private AdminExerciseWebMapper() {
    }

    public static ExerciseCommand.Create toCommand(AdminExerciseRequest.Create request) {
        return new ExerciseCommand.Create(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo()
        );
    }

    public static ExerciseCommand.Update toCommand(AdminExerciseRequest.Update request) {
        return new ExerciseCommand.Update(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo()
        );
    }

    public static ExerciseCommand.Search toCommand(AdminExerciseRequest.Search request) {
        return new ExerciseCommand.Search(
                request.category(),
                request.from(),
                request.to(),
                request.page(),
                request.size()
        );
    }

    public static AdminExerciseResponse.Created toResponse(ExerciseResult.Created result) {
        return new AdminExerciseResponse.Created(result.id());
    }

    public static AdminExerciseResponse.Info toResponse(ExerciseResult.Info result) {
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

    public static List<AdminExerciseResponse.Info> toResponseList(List<ExerciseResult.Info> results) {
        return results.stream().map(AdminExerciseWebMapper::toResponse).toList();
    }

    public static ExerciseCommand.Search toCommand(String category, LocalDate from, LocalDate to, int page, int size) {
        return new ExerciseCommand.Search(
                category,
                from,
                to,
                page,
                size
        );
    }
}
