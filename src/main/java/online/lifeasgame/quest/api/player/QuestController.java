package online.lifeasgame.quest.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.api.player.mapper.QuestWebMapper;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.api.player.spec.QuestSpecV1;
import online.lifeasgame.quest.application.QuestFacade;
import online.lifeasgame.quest.application.result.QuestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
