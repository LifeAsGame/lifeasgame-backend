package online.lifeasgame.quest.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.quest.api.player.mapper.QuestWebMapper;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.application.BackendJourneyEvidenceService;
import online.lifeasgame.quest.domain.JourneyEvidence;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/quests/{questCode}")
public class BackendJourneyEvidenceController {
    private final BackendJourneyEvidenceService service;

    public record MemoRequest(@NotNull String memo) {}
    public record LifeLogRequest(@NotNull @Positive Long lifeLogId) {}
    public record DeploymentRequest(@NotNull String url, @NotNull String description) {}

    @PutMapping("/evidence/memo")
    public ResponseEntity<ApiResponse<QuestResponse.Acceptance>> memo(
            @PathVariable String questCode, @Valid @RequestBody MemoRequest request) {
        return ApiResponses.ok(QuestWebMapper.toAcceptance(service.linkMemo(questCode, request.memo())));
    }

    @PutMapping("/evidence/life-log")
    public ResponseEntity<ApiResponse<QuestResponse.Acceptance>> lifeLog(
            @PathVariable String questCode, @Valid @RequestBody LifeLogRequest request) {
        return ApiResponses.ok(QuestWebMapper.toAcceptance(service.linkLifeLog(questCode, request.lifeLogId())));
    }

    @PutMapping("/evidence/deployment")
    public ResponseEntity<ApiResponse<QuestResponse.Acceptance>> deployment(
            @PathVariable String questCode, @Valid @RequestBody DeploymentRequest request) {
        return ApiResponses.ok(QuestWebMapper.toAcceptance(service.linkDeployment(
                questCode, request.url(), request.description())));
    }

    @GetMapping("/evidence")
    public ResponseEntity<ApiResponse<JourneyEvidence>> evidence(@PathVariable String questCode) {
        return ApiResponses.ok(service.evidence(questCode));
    }

    @DeleteMapping("/evidence")
    public ResponseEntity<ApiResponse<QuestResponse.Acceptance>> unlink(@PathVariable String questCode) {
        return ApiResponses.ok(QuestWebMapper.toAcceptance(service.unlink(questCode)));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<QuestResponse.Acceptance>> complete(@PathVariable String questCode) {
        return ApiResponses.ok(QuestWebMapper.toAcceptance(service.complete(questCode)));
    }
}
