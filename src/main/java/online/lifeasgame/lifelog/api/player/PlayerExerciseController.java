package online.lifeasgame.lifelog.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.api.player.mapper.PlayerExerciseWebMapper;
import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerExerciseResponse;
import online.lifeasgame.lifelog.api.player.spec.PlayerExerciseSpecV1;
import online.lifeasgame.lifelog.application.ExerciseLogFacade;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/exercises")
public class PlayerExerciseController implements PlayerExerciseSpecV1 {

    private final ExerciseLogFacade exerciseLogFacade;

    @GetMapping("/recent")
    @Override
    public ResponseEntity<List<PlayerExerciseResponse.Info>> recent(@RequestParam(defaultValue = "20") Integer limit) {
        List<ExerciseResult.Info> results = exerciseLogFacade.recent(limit);
        return ResponseEntity.ok(PlayerExerciseWebMapper.toInfos(results));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlayerExerciseResponse.Info>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<ExerciseResult.Info> results = exerciseLogFacade.search(
                PlayerExerciseWebMapper.toSearchCommand(category, from, to, page, size)
        );
        return ResponseEntity.ok(PlayerExerciseWebMapper.toInfos(results));
    }

    @PostMapping
    @Override
    public ResponseEntity<PlayerExerciseResponse.Created> create(
            @Valid @RequestBody PlayerExerciseRequest.Create request
    ) {
        ExerciseResult.Created result = exerciseLogFacade.create(PlayerExerciseWebMapper.toCreateCommand(request));
        return ResponseEntity.ok(PlayerExerciseWebMapper.toCreated(result));
    }

    @PostMapping("/{exerciseId}")
    @Override
    public ResponseEntity<PlayerExerciseResponse.Info> update(
            @PathVariable Long exerciseId,
            @Valid @RequestBody PlayerExerciseRequest.Update request
    ) {
        ExerciseResult.Info result = exerciseLogFacade.update(exerciseId, PlayerExerciseWebMapper.toUpdateCommand(request));
        return ResponseEntity.ok(PlayerExerciseWebMapper.toInfo(result));
    }
}
