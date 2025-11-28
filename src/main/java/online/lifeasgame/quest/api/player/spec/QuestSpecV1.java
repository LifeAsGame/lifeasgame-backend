package online.lifeasgame.quest.api.player.spec;

import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.domain.QuestStatus;
import org.springframework.http.ResponseEntity;

public interface QuestSpecV1 {
    ResponseEntity<QuestResponse.Acceptances> list(QuestStatus status);

    ResponseEntity<QuestResponse.PlayerQuest> detail(String questCode);
}
