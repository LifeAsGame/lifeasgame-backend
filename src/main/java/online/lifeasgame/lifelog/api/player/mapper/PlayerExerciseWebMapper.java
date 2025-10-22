package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerExerciseResponse;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.result.ExerciseResult;

import java.time.LocalDate;
import java.util.List;

public final class PlayerExerciseWebMapper {
    private PlayerExerciseWebMapper() {
    }

    public static ExerciseCommand.Create toCommand(PlayerExerciseRequest.Create request) {
        return new ExerciseCommand.Create(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo()
        );
    }

    public static ExerciseCommand.Update toCommand(PlayerExerciseRequest.Update request) {
        return new ExerciseCommand.Update(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo()
        );
    }

    public static ExerciseCommand.Search toCommand(PlayerExerciseRequest.Search request) {
        return new ExerciseCommand.Search(
                request.category(),
                request.from(),
                request.to(),
                request.page(),
                request.size()
        );
    }

    public static PlayerExerciseResponse.Created toResponse(ExerciseResult.Created result) {
        return new PlayerExerciseResponse.Created(result.id());
    }

    public static PlayerExerciseResponse.Info toResponse(ExerciseResult.Info result) {
        return new PlayerExerciseResponse.Info(
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

    public static List<PlayerExerciseResponse.Info> toResponseList(List<ExerciseResult.Info> results) {
        return results.stream().map(PlayerExerciseWebMapper::toResponse).toList();
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
