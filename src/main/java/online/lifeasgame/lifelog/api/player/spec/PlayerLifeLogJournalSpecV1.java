package online.lifeasgame.lifelog.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.response.PlayerLifeLogJournalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "LifeLog Journal API V1 (Player)")
public interface PlayerLifeLogJournalSpecV1 {

    @Operation(summary = "canonical Journal timeline 조회")
    ResponseEntity<ApiResponse<PlayerLifeLogJournalResponse.Page>> list(
            @RequestParam(required = false) @Positive Long primaryRoleId,
            @RequestParam(required = false) String subtype,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(summary = "canonical Journal 상세 조회")
    ResponseEntity<ApiResponse<PlayerLifeLogJournalResponse.Detail>> detail(
            @PathVariable @Positive Long lifeLogId
    );
}
