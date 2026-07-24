package online.lifeasgame.quest.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.quest.api.admin.mapper.AdminQuestWebMapper;
import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import online.lifeasgame.quest.api.admin.spec.AdminQuestSpecV1;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.result.QuestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/quests")
public class AdminQuestController implements AdminQuestSpecV1 {

    private final QuestService questService;

    @Override
    @GetMapping("/catalog")
    public ResponseEntity<AdminQuestResponse.Blueprints> catalog() {
        List<QuestResult.Blueprint> results = questService.getCatalog();
        return ResponseEntity.ok(AdminQuestWebMapper.toBlueprints(results));
    }

    @Override
    @GetMapping("/definitions")
    public ResponseEntity<AdminQuestResponse.Definitions> definitions() {
        List<QuestResult.Definition> results = questService.getDefinitions();
        return ResponseEntity.ok(AdminQuestWebMapper.toDefinitions(results));
    }

    @Override
    @PostMapping("/definitions")
    public ResponseEntity<AdminQuestResponse.Definition> ensure(
            @Valid @RequestBody AdminQuestRequest.Ensure request
    ) {
        QuestResult.Definition result = questService.ensureDefinition(AdminQuestWebMapper.toEnsureCommand(request.code()));
        return ResponseEntity.ok(AdminQuestWebMapper.toDefinition(result));
    }

    @Override
    @GetMapping("/definitions/{questCode}")
    public ResponseEntity<AdminQuestResponse.Definition> definition(@PathVariable String questCode) {
        QuestResult.Definition result = questService.getDefinition(AdminQuestWebMapper.toDefinitionCommand(questCode));
        return ResponseEntity.ok(AdminQuestWebMapper.toDefinition(result));
    }

    @Override
    @PatchMapping("/definitions/{questCode}")
    public ResponseEntity<AdminQuestResponse.Definition> update(
            @PathVariable String questCode,
            @Valid @RequestBody AdminQuestRequest.Update request
    ) {
        QuestResult.Definition result = questService.updateDefinition(
                AdminQuestWebMapper.toUpdateCommand(questCode, request)
        );

        return ResponseEntity.ok(AdminQuestWebMapper.toDefinition(result));
    }

    @Override
    @GetMapping("/{questCode}/acceptances")
    public ResponseEntity<AdminQuestResponse.Acceptances> acceptances(
            @PathVariable String questCode,
            @RequestParam(required = false) String status
    ) {
        List<QuestResult.Acceptance> results = questService.questAcceptances(
                AdminQuestWebMapper.toAcceptancesCommand(questCode, status)
        );

        return ResponseEntity.ok(AdminQuestWebMapper.toAcceptances(results));
    }

    @Override
    @GetMapping("/acceptances/{acceptanceId}")
    public ResponseEntity<AdminQuestResponse.Acceptance> acceptance(@PathVariable Long acceptanceId) {
        QuestResult.Acceptance result = questService.acceptance(
                AdminQuestWebMapper.toAcceptanceCommand(acceptanceId)
        );

        return ResponseEntity.ok(AdminQuestWebMapper.toAcceptance(result));
    }

    @Override
    @PatchMapping("/acceptances/{acceptanceId}/progress")
    public ResponseEntity<ApiResponse<AdminQuestResponse.Acceptance>> adjustProgress(
            @PathVariable Long acceptanceId,
            @Valid @RequestBody AdminQuestRequest.AdjustProgress request
    ) {
        QuestResult.Acceptance result = questService.adjustAcceptanceProgress(
                acceptanceId,
                AdminQuestWebMapper.toAdjustProgressCommand(request)
        );
        return ApiResponses.ok(AdminQuestWebMapper.toAcceptance(result));
    }

    @Override
    @PatchMapping("/acceptances/{acceptanceId}/status")
    public ResponseEntity<ApiResponse<AdminQuestResponse.Acceptance>> changeStatus(
            @PathVariable Long acceptanceId,
            @Valid @RequestBody AdminQuestRequest.ChangeStatus request
    ) {
        QuestResult.Acceptance result = questService.changeAcceptanceStatus(
                acceptanceId,
                AdminQuestWebMapper.toChangeStatusCommand(request)
        );
        return ApiResponses.ok(AdminQuestWebMapper.toAcceptance(result));
    }
}
