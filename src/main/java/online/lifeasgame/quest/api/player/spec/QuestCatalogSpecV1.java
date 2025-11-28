package online.lifeasgame.quest.api.player.spec;

import online.lifeasgame.quest.api.player.response.QuestResponse;
import org.springframework.http.ResponseEntity;

public interface QuestCatalogSpecV1 {
    ResponseEntity<QuestResponse.Blueprints> catalog();
}
