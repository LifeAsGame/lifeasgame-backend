package online.lifeasgame.quest.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.api.player.mapper.QuestWebMapper;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.api.player.spec.QuestCatalogSpecV1;
import online.lifeasgame.quest.application.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quests")
public class QuestCatalogController implements QuestCatalogSpecV1 {

    private final QuestService questService;

    @GetMapping("/catalog")
    @Override
    public ResponseEntity<QuestResponse.Blueprints> catalog() {
        return ResponseEntity.ok(QuestWebMapper.toBlueprints(questService.catalog()));
    }
}
