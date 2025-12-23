package online.lifeasgame.quest.api.admin.spec;

import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import org.springframework.http.ResponseEntity;

public interface AdminQuestSpecV1 {
    ResponseEntity<AdminQuestResponse.Blueprints> catalog();

    ResponseEntity<AdminQuestResponse.Definitions> definitions();

    ResponseEntity<AdminQuestResponse.Definition> ensure(AdminQuestRequest.Ensure request);

    ResponseEntity<AdminQuestResponse.Definition> definition(String questCode);

    ResponseEntity<AdminQuestResponse.Definition> update(String questCode, AdminQuestRequest.Update request);

    ResponseEntity<AdminQuestResponse.Acceptances> acceptances(String questCode, String status);

    ResponseEntity<AdminQuestResponse.Acceptance> acceptance(Long acceptanceId);
}
