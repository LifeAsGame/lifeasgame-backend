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
            description = "저장된 Reward Settlement와 Line 처리 상태를 조회합니다. GOLD line amount는 입금액입니다. "
                    + "COMPLETED만 지급 완료이며 PENDING은 정산 대기, PARTIAL_FAILED는 일부 실패입니다. "
                    + "NOT_ELIGIBLE은 계정 지급 자격이 이미 사용되어 이번 정산에는 지급 line이 없음을 뜻합니다. "
                    + "Mailbox, Inventory 또는 현재 소유 상태를 나타내지 않습니다."
    )
    ResponseEntity<ApiResponse<RewardSettlementResponse.Detail>>
    getQuestCompletionSettlement(
            @PathVariable Long questAcceptanceId
    );
}
