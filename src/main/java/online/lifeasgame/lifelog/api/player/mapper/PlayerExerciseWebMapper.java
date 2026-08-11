package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerExerciseResponse;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.query.ExerciseQuery;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.result.ExerciseResult;

import java.time.LocalDate;
import java.util.List;

public final class PlayerExerciseWebMapper {

    private PlayerExerciseWebMapper() {
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

    public static List<PlayerExerciseResponse.Info> toInfos(List<ExerciseResult.Info> results) {
        return results.stream().map(PlayerExerciseWebMapper::toInfo).toList();
    }

    public static ExerciseCommand.Create toCreateCommand(PlayerExerciseRequest.Create request) {
        return new ExerciseCommand.Create(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo(),
                new LifeLogRecordMetadataCommand(
                        request.lifeLogSubtype(),
                        request.reflectionScope(),
                        request.primaryRoleId(),
                        request.roleEventId()
                )
        );
    }

    public static PlayerExerciseResponse.Created toCreated(ExerciseResult.Created result) {
        return new PlayerExerciseResponse.Created(result.id());
    }

    public static ExerciseCommand.Update toUpdateCommand(PlayerExerciseRequest.Update request) {
        return new ExerciseCommand.Update(
                request.category(),
                request.durationMinutes(),
                request.distanceKm(),
                request.calories(),
                request.exercisedOn(),
                request.memo()
        );
    }

    public static PlayerExerciseResponse.Info toInfo(ExerciseResult.Info result) {
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

    public static PlayerExerciseResponse.Deleted toDeleted(ExerciseResult.Deleted result) {
        return new PlayerExerciseResponse.Deleted(
                result.exerciseId()
        );
    }
}
