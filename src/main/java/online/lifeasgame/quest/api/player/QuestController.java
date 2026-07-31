package online.lifeasgame.quest.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.quest.api.player.mapper.QuestWebMapper;
import online.lifeasgame.quest.api.player.request.QuestRequest;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.api.player.spec.QuestSpecV1;
import online.lifeasgame.quest.application.QuestFacade;
import online.lifeasgame.quest.application.result.QuestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/quests")
public class QuestController implements QuestSpecV1 {

    private final QuestFacade questFacade;

    @Override
    @GetMapping
    public ResponseEntity<QuestResponse.Acceptances> list(
            @RequestParam(required = false) String status
    ) {
        List<QuestResult.Acceptance> results = questFacade.list(QuestWebMapper.toListCommand(status));
        return ResponseEntity.ok(QuestWebMapper.toAcceptances(results));
    }

    @Override
    @GetMapping("/{questCode}")
    public ResponseEntity<QuestResponse.PlayerQuest> detail(
            @PathVariable String questCode
    ) {
        QuestResult.PlayerQuest result = questFacade.detail(QuestWebMapper.toPlayerQuestCommand(questCode));
        return ResponseEntity.ok(QuestWebMapper.toPlayerQuest(result));
    }

    @Override
    @PostMapping("/{questCode}")
    public ResponseEntity<ApiResponse<QuestResponse.Acceptance>> accept(
            @PathVariable String questCode,
            @Valid @RequestBody QuestRequest.Accept request
    ) {
        QuestResult.Acceptance result = questFacade.accept(QuestWebMapper.toAcceptCommand(questCode, request));
        return ApiResponses.created(
                URI.create("/api/v1/players/quests"),
                QuestWebMapper.toAcceptance(result)
        );
    }

    @Override
    @PostMapping("/{questCode}/manual-check")
    public ResponseEntity<ApiResponse<QuestResponse.Acceptance>> manualCheck(
            @PathVariable String questCode
    ) {
        QuestResult.Acceptance result = questFacade.manualCheck(
                QuestWebMapper.toManualCheckCommand(questCode)
        );
        return ApiResponses.ok(QuestWebMapper.toAcceptance(result));
    }

    @Override
    @DeleteMapping("/{questCode}")
    public ResponseEntity<ApiResponse<QuestResponse.Canceled>> cancel(
            @PathVariable String questCode,
            @Valid @RequestBody QuestRequest.Cancel request
    ) {
        QuestResult.Canceled result = questFacade.cancel(QuestWebMapper.toCancelCommand(questCode, request));
        return ApiResponses.deleted(QuestWebMapper.toCanceled(result));
    }
}
