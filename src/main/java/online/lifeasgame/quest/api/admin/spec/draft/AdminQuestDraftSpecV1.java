package online.lifeasgame.quest.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface AdminQuestDraftSpecV1 {

    @Operation(summary = "Acceptance 검색(운영)", description = "playerId/questCode/status로 Acceptance를 검색합니다.")
    ResponseEntity<ApiResponse<AdminQuestResponse.Acceptances>> searchAcceptances(
            @RequestParam(name = "playerId", required = false) Long playerId,
            @RequestParam(name = "questCode", required = false) String questCode,
            @RequestParam(name = "status", required = false) String status
    );

    @Operation(summary = "보상 파이프라인 강제 트리거", description = "DONE 상태 Acceptance에 대해 보상 이벤트를 강제 발행/재발행합니다. (멱등키 권장)")
    ResponseEntity<ApiResponse<AdminQuestResponse.RewardTriggered>> triggerReward(
            @PathVariable Long acceptanceId,
            @Valid @RequestBody AdminQuestRequest.TriggerReward request
    );

    @Operation(summary = "Quest 메타 조회", description = "운영 화면 필터/표시에 필요한 enum 메타를 반환합니다.")
    ResponseEntity<ApiResponse<AdminQuestResponse.Meta>> meta();
}
