package online.lifeasgame.lifelog.quick.api;

import online.lifeasgame.lifelog.api.player.mapper.PlayerCollectionWebMapper;
import online.lifeasgame.lifelog.api.player.mapper.PlayerExerciseWebMapper;
import online.lifeasgame.lifelog.api.player.mapper.PlayerMediaLogWebMapper;
import online.lifeasgame.lifelog.quick.application.QuickRecordCommand;
import online.lifeasgame.lifelog.quick.application.QuickRecordResult;

public final class QuickRecordWebMapper {

    private QuickRecordWebMapper() {
    }

    public static QuickRecordCommand.Create toCommand(
            QuickRecordRequest.Create request
    ) {
        return new QuickRecordCommand.Create(
                request.type(),
                request.lifeLogSubtype(),
                request.reflectionScope(),
                request.collection() == null
                        ? null
                        : PlayerCollectionWebMapper.toCreateCommand(
                                request.collection()
                        ),
                request.exercise() == null
                        ? null
                        : PlayerExerciseWebMapper.toCreateCommand(
                                request.exercise()
                        ),
                request.media() == null
                        ? null
                        : PlayerMediaLogWebMapper.toCreateCommand(
                                request.media()
                        )
        );
    }

    public static QuickRecordResponse.Recorded toResponse(
            QuickRecordResult.Recorded result
    ) {
        return new QuickRecordResponse.Recorded(
                result.sourceType().name(),
                result.sourceId(),
                result.recordedAt(),
                result.replay()
        );
    }
}
