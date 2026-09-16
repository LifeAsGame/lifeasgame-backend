package online.lifeasgame.reward.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.reward.api.response.RewardSettlementResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Reward Settlement API V1 (Player)")
public interface RewardSettlementApiSpecV1 {

    @Operation(
            summary = "내 Quest 완료 Reward Settlement 조회",
            description = "저장된 Reward Settlement와 Line 처리 상태를 조회합니다. "
                    + "Mailbox, Inventory 또는 현재 소유 상태를 나타내지 않습니다."
    )
    ResponseEntity<ApiResponse<RewardSettlementResponse.Detail>>
    getQuestCompletionSettlement(
            @PathVariable Long questAcceptanceId
    );
}
