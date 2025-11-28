package online.lifeasgame.quest.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.api.admin.mapper.AdminQuestWebMapper;
import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import online.lifeasgame.quest.api.admin.spec.AdminQuestSpecV1;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.domain.QuestStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/quests")
public class AdminQuestController implements AdminQuestSpecV1 {

    private final QuestService questService;

    @Override
    @GetMapping("/catalog")
    public ResponseEntity<AdminQuestResponse.Blueprints> catalog() {
        return ResponseEntity.ok(AdminQuestWebMapper.toBlueprints(questService.catalog()));
    }

    @Override
    @GetMapping("/definitions")
    public ResponseEntity<AdminQuestResponse.Definitions> definitions() {
        return ResponseEntity.ok(AdminQuestWebMapper.toDefinitions(questService.definitions()));
    }

    @Override
    @PostMapping("/definitions")
    public ResponseEntity<AdminQuestResponse.Definition> ensure(@Valid @RequestBody AdminQuestRequest.Ensure request) {
        return ResponseEntity.ok(AdminQuestWebMapper.toDefinition(questService.ensureDefinition(AdminQuestWebMapper.toEnsureCommand(request.code()))));
    }

    @Override
    @GetMapping("/definitions/{questCode}")
    public ResponseEntity<AdminQuestResponse.Definition> definition(@PathVariable String questCode) {
        return ResponseEntity.ok(AdminQuestWebMapper.toDefinition(questService.definition(AdminQuestWebMapper.toDefinitionCommand(questCode))));
    }

    @Override
    @PatchMapping("/definitions/{questCode}")
    public ResponseEntity<AdminQuestResponse.Definition> update(
            @PathVariable String questCode,
            @RequestBody AdminQuestRequest.Update request
    ) {
        return ResponseEntity.ok(AdminQuestWebMapper.toDefinition(questService.updateDefinition(AdminQuestWebMapper.toUpdateCommand(questCode, request))));
    }

    @Override
    @GetMapping("/{questCode}/acceptances")
    public ResponseEntity<AdminQuestResponse.Acceptances> acceptances(
            @PathVariable String questCode,
            @RequestParam(required = false) QuestStatus status
    ) {
        return ResponseEntity.ok(AdminQuestWebMapper.toAcceptances(questService.questAcceptances(AdminQuestWebMapper.toAcceptancesCommand(questCode, status))));
    }

    @Override
    @GetMapping("/acceptances/{acceptanceId}")
    public ResponseEntity<AdminQuestResponse.Acceptance> acceptance(@PathVariable Long acceptanceId) {
        return ResponseEntity.ok(AdminQuestWebMapper.toAcceptance(questService.acceptance(AdminQuestWebMapper.toAcceptanceCommand(acceptanceId))));
    }
}
