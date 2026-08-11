package online.lifeasgame.lifelog.quick.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;

public final class QuickRecordRequest {

    private QuickRecordRequest() {
    }

    public record Create(
            @NotBlank String type,
            String lifeLogSubtype,
            String reflectionScope,
            @Positive Long primaryRoleId,
            @Positive Long roleEventId,
            @Valid PlayerCollectionRequest.Create collection,
            @Valid PlayerExerciseRequest.Create exercise,
            @Valid PlayerMediaLogRequest.Create media
    ) {
        public Create(
                String type,
                String lifeLogSubtype,
                String reflectionScope,
                PlayerCollectionRequest.Create collection,
                PlayerExerciseRequest.Create exercise,
                PlayerMediaLogRequest.Create media
        ) {
            this(
                    type,
                    lifeLogSubtype,
                    reflectionScope,
                    null,
                    null,
                    collection,
                    exercise,
                    media
            );
        }

        public Create(
                String type,
                PlayerCollectionRequest.Create collection,
                PlayerExerciseRequest.Create exercise,
                PlayerMediaLogRequest.Create media
        ) {
            this(
                    type,
                    null,
                    null,
                    null,
                    null,
                    collection,
                    exercise,
                    media
            );
        }
    }
}
