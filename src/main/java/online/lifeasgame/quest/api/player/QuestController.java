package online.lifeasgame.quest.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.api.player.mapper.QuestWebMapper;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.api.player.spec.QuestSpecV1;
import online.lifeasgame.quest.application.QuestFacade;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.domain.QuestStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players/{playerId}/quests")
public class QuestController implements QuestSpecV1 {

    private final QuestFacade questFacade;

    @GetMapping
    @Override
    public ResponseEntity<QuestResponse.Acceptances> list(
            @RequestParam(required = false) QuestStatus status
    ) {
        return ResponseEntity.ok(QuestWebMapper.toAcceptances(questFacade.list(new QuestCommand.PlayerQuests(status))));
    }

    @GetMapping("/{questCode}")
    @Override
    public ResponseEntity<QuestResponse.PlayerQuest> detail(
            @PathVariable String questCode
    ) {
        return ResponseEntity.ok(
                QuestWebMapper.toPlayerQuest(questFacade.detail(new QuestCommand.PlayerQuest(questCode)))
        );
    }
}
