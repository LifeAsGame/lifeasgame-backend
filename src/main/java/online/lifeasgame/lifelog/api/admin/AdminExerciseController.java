package online.lifeasgame.lifelog.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.api.admin.mapper.AdminExerciseWebMapper;
import online.lifeasgame.lifelog.api.admin.request.AdminExerciseRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminExerciseResponse;
import online.lifeasgame.lifelog.api.admin.spec.AdminExerciseSpecV1;
import online.lifeasgame.lifelog.application.ExerciseLogService;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
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

    @PostMapping("/{playerId}/exercises")
    @Override
    public ResponseEntity<AdminExerciseResponse.Created> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminExerciseRequest.Create request
    ) {
        ExerciseResult.Created created = exerciseLogService.create(playerId, AdminExerciseWebMapper.toCommand(request));
        return ResponseEntity.ok(AdminExerciseWebMapper.toResponse(created));
    }

    @PostMapping("/{playerId}/exercises/{exerciseId}")
    @Override
    public ResponseEntity<AdminExerciseResponse.Info> update(
            @PathVariable Long playerId,
            @PathVariable Long exerciseId,
            @Valid @RequestBody AdminExerciseRequest.Update request
    ) {
        ExerciseResult.Info info = exerciseLogService.update(
                playerId,
                exerciseId,
                AdminExerciseWebMapper.toCommand(request)
        );
        return ResponseEntity.ok(AdminExerciseWebMapper.toResponse(info));
    }

    @GetMapping("/{playerId}/exercises/recent")
    @Override
    public ResponseEntity<List<AdminExerciseResponse.Info>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<ExerciseResult.Info> infos = exerciseLogService.recent(playerId, limit);
        return ResponseEntity.ok(AdminExerciseWebMapper.toResponseList(infos));
    }

    @GetMapping("/{playerId}/exercises/search")
    public ResponseEntity<List<AdminExerciseResponse.Info>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ExerciseResult.Info> infos = exerciseLogService.search(
                playerId,
                AdminExerciseWebMapper.toCommand(
                        category,
                        from,
                        to,
                        page,
                        size
                )
        );
        return ResponseEntity.ok(AdminExerciseWebMapper.toResponseList(infos));
    }
}
