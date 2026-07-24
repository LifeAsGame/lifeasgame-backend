package online.lifeasgame.lifelog.quick.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;

public final class QuickRecordRequest {

    private QuickRecordRequest() {
    }

    public record Create(
            @NotBlank String type,
            @Valid PlayerCollectionRequest.Create collection,
            @Valid PlayerExerciseRequest.Create exercise,
            @Valid PlayerMediaLogRequest.Create media
    ) {
    }
}
