package online.lifeasgame.quest.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Quest API V1")
public interface AdminQuestSpecV1 {

    @Operation(summary = "Quest Blueprint 목록", description = "서버에 정의된 Blueprint(카탈로그 소스) 목록을 조회합니다.")
    ResponseEntity<AdminQuestResponse.Blueprints> catalog();

    @Operation(summary = "Quest Definition 목록", description = "DB에 저장된 Quest Definition 목록을 조회합니다.")
    ResponseEntity<AdminQuestResponse.Definitions> definitions();

    @Operation(summary = "Quest Definition 생성/보장", description = "Blueprint code 기준으로 Quest Definition을 생성(또는 존재 보장)합니다.")
    ResponseEntity<AdminQuestResponse.Definition> ensure(AdminQuestRequest.Ensure request);

    @Operation(summary = "Quest Definition 단건 조회", description = "questCode로 Quest Definition을 조회합니다.")
    ResponseEntity<AdminQuestResponse.Definition> definition(String questCode);

    @Operation(summary = "Quest Definition 수정", description = "타겟/보상/반복/마감일을 수정합니다.")
    ResponseEntity<AdminQuestResponse.Definition> update(String questCode, AdminQuestRequest.Update request);

    @Operation(summary = "Quest Acceptance 목록(quest 기준)", description = "특정 questCode에 대한 Acceptance 목록을 조회합니다. (status 필터 가능)")
    ResponseEntity<AdminQuestResponse.Acceptances> acceptances(String questCode, String status);

    @Operation(summary = "Quest Acceptance 단건 조회", description = "acceptanceId로 Acceptance 상세를 조회합니다.")
    ResponseEntity<AdminQuestResponse.Acceptance> acceptance(Long acceptanceId);

    @Operation(summary = "Acceptance 진행도 조정", description = "운영/CS 목적으로 Acceptance 진행도를 SET/ADD 방식으로 조정합니다. (멱등키 권장)")
    ResponseEntity<ApiResponse<AdminQuestResponse.Acceptance>> adjustProgress(
            @PathVariable Long acceptanceId,
            @Valid @RequestBody AdminQuestRequest.AdjustProgress request
    );

    @Operation(summary = "Acceptance 상태 변경", description = "Acceptance 상태를 강제로 변경합니다. (예: CANCELED, DONE) (멱등키 권장)")
    ResponseEntity<ApiResponse<AdminQuestResponse.Acceptance>> changeStatus(
            @PathVariable Long acceptanceId,
            @Valid @RequestBody AdminQuestRequest.ChangeStatus request
    );
}
