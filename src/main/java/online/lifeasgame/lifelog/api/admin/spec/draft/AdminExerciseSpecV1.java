package online.lifeasgame.lifelog.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.admin.response.AdminExerciseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminExerciseSpecV1 {

    @Operation(summary = "운동 삭제(관리자, 플레이어 스코프)")
    ResponseEntity<ApiResponse<AdminExerciseResponse.Deleted>> delete(
            @PathVariable Long playerId,
            @PathVariable Long exerciseId
    );
}
