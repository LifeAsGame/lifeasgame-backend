package online.lifeasgame.quest.api.player.spec;

import online.lifeasgame.quest.api.player.response.QuestResponse;
import org.springframework.http.ResponseEntity;

public interface QuestSpecV1 {
    ResponseEntity<QuestResponse.Acceptances> list(String status);

    ResponseEntity<QuestResponse.PlayerQuest> detail(String questCode);
}
