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

    @Operation(summary = "보상 요청(수령 트리거)", description = "COMPLETED 퀘스트의 보상 파이프라인을 요청/재요청합니다. (멱등키 권장)")
    ResponseEntity<ApiResponse<QuestResponse.RewardClaimed>> claimReward(
            @PathVariable String questCode,
            @Valid @RequestBody QuestRequest.ClaimReward request
    );
}
