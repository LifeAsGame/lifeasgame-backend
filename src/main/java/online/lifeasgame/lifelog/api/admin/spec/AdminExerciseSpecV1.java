package online.lifeasgame.lifelog.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.lifelog.api.admin.request.AdminExerciseRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminExerciseResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "LifeLog Exercise API V1 (Admin)")
public interface AdminExerciseSpecV1 {
    @Operation(summary = "운동 등록(관리자, 플레이어 스코프)")
    ResponseEntity<AdminExerciseResponse.Created> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminExerciseRequest.Create request
    );

    @Operation(summary = "운동 수정(관리자, 플레이어 스코프)")
    ResponseEntity<AdminExerciseResponse.Info> update(
            Long playerId,
            Long exerciseId,
            AdminExerciseRequest.Update request
    );

    @Operation(summary = "최근 조회(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminExerciseResponse.Info>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") Integer limit
    );

    @Operation(summary = "검색(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminExerciseResponse.Info>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
