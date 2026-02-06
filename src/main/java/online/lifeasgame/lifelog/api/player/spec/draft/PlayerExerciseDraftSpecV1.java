package online.lifeasgame.lifelog.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.response.PlayerExerciseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface PlayerExerciseDraftSpecV1 {

    @Operation(summary = "운동 삭제")
    ResponseEntity<ApiResponse<PlayerExerciseResponse.Deleted>> delete(
            @PathVariable Long exerciseId
    );
}
