package online.lifeasgame.quest.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.quest.api.player.request.QuestRequest;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface QuestDraftSpecV1 {

    @Operation(summary = "퀘스트 수락", description = "플레이어가 퀘스트를 수락(시작)합니다. (멱등키 권장)")
    ResponseEntity<ApiResponse<QuestResponse.Accepted>> accept(
            @PathVariable String questCode,
            @Valid @RequestBody QuestRequest.Accept request
    );

    @Operation(summary = "퀘스트 포기", description = "진행중 퀘스트를 포기(CANCELED) 처리합니다. (멱등키 권장)")
    ResponseEntity<ApiResponse<QuestResponse.Acceptance>> cancel(
            @PathVariable String questCode,
            @Valid @RequestBody QuestRequest.Cancel request
    );

    @Operation(summary = "보상 요청(수령 트리거)", description = "DONE 퀘스트의 보상 파이프라인을 요청/재요청합니다. (멱등키 권장)")
    ResponseEntity<ApiResponse<QuestResponse.RewardClaimed>> claimReward(
            @PathVariable String questCode,
            @Valid @RequestBody QuestRequest.ClaimReward request
    );
}
