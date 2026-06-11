package online.lifeasgame.lifelog.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerExerciseResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "LifeLog Exercise API V1 (Player)")
public interface PlayerExerciseSpecV1 {

    @Operation(summary = "최근 조회")
    ResponseEntity<List<PlayerExerciseResponse.Info>> recent(
            @RequestParam(defaultValue = "20") Integer limit
    );

    @Operation(summary = "검색", description = "page/size 기반 검색. UI 페이징을 위해 Page 응답을 권장합니다.")
    ResponseEntity<List<PlayerExerciseResponse.Info>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "운동 등록")
    ResponseEntity<PlayerExerciseResponse.Created> create(
            @Valid @RequestBody PlayerExerciseRequest.Create request
    );

    @Operation(summary = "운동 수정")
    ResponseEntity<PlayerExerciseResponse.Info> update(
            @PathVariable Long exerciseId,
            @Valid @RequestBody PlayerExerciseRequest.Update request
    );

    @Operation(summary = "단건 조회")
    ResponseEntity<ApiResponse<PlayerExerciseResponse.Info>> get(
            @PathVariable Long exerciseId
    );

    @Operation(summary = "운동 삭제")
    ResponseEntity<ApiResponse<PlayerExerciseResponse.Deleted>> delete(
            @PathVariable Long exerciseId
    );
}
