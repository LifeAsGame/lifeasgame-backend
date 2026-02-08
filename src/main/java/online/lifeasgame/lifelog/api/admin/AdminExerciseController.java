package online.lifeasgame.lifelog.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.admin.mapper.AdminExerciseWebMapper;
import online.lifeasgame.lifelog.api.admin.request.AdminExerciseRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminExerciseResponse;
import online.lifeasgame.lifelog.api.admin.spec.AdminExerciseSpecV1;
import online.lifeasgame.lifelog.application.ExerciseLogService;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminExerciseController implements AdminExerciseSpecV1 {

    private final ExerciseLogService exerciseLogService;

    @Override
    @GetMapping("/{playerId}/exercises/recent")
    public ResponseEntity<List<AdminExerciseResponse.Info>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        List<ExerciseResult.Info> results = exerciseLogService.recent(playerId, limit);
        return ResponseEntity.ok(AdminExerciseWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{playerId}/exercises/search")
    public ResponseEntity<List<AdminExerciseResponse.Info>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ExerciseResult.Info> results = exerciseLogService.search(
                playerId,
                AdminExerciseWebMapper.toSearchCommand(category, from, to, page, size)
        );

        return ResponseEntity.ok(AdminExerciseWebMapper.toInfos(results));
    }

    @Override
    @PostMapping("/{playerId}/exercises")
    public ResponseEntity<AdminExerciseResponse.Created> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminExerciseRequest.Create request
    ) {
        ExerciseResult.Created result = exerciseLogService.create(
                playerId,
                AdminExerciseWebMapper.toCreateCommand(request)
        );

        return ResponseEntity.ok(AdminExerciseWebMapper.toCreated(result));
    }

    @Override
    @PostMapping("/{playerId}/exercises/{exerciseId}")
    public ResponseEntity<AdminExerciseResponse.Info> update(
            @PathVariable Long playerId,
            @PathVariable Long exerciseId,
            @Valid @RequestBody AdminExerciseRequest.Update request
    ) {
        ExerciseResult.Info result = exerciseLogService.update(
                playerId,
                exerciseId,
                AdminExerciseWebMapper.toUpdateCommand(request)
        );

        return ResponseEntity.ok(AdminExerciseWebMapper.toInfo(result));
    }

    @Override
    @GetMapping("/{playerId}/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<AdminExerciseResponse.Info>> get(
            @PathVariable Long playerId,
            @PathVariable Long exerciseId
    ) {
        ExerciseResult.Info result = exerciseLogService.getExercise(playerId, exerciseId);
        return ApiResponses.ok(AdminExerciseWebMapper.toInfo(result));
    }
}
