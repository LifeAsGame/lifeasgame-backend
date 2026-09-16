package online.lifeasgame.reward.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.reward.api.mapper.RewardSettlementWebMapper;
import online.lifeasgame.reward.api.response.RewardSettlementResponse;
import online.lifeasgame.reward.api.spec.RewardSettlementApiSpecV1;
import online.lifeasgame.reward.application.RewardSettlementQueryService;
import online.lifeasgame.reward.application.result.RewardSettlementResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reward-settlements")
public class RewardSettlementController implements RewardSettlementApiSpecV1 {

    private final RewardSettlementQueryService queryService;

    @Override
    @GetMapping("/quest-completions/{questAcceptanceId}")
    public ResponseEntity<ApiResponse<RewardSettlementResponse.Detail>>
    getQuestCompletionSettlement(@PathVariable Long questAcceptanceId) {
        RewardSettlementResult.Detail result =
                queryService.getQuestCompletionSettlement(questAcceptanceId);
        return ApiResponses.ok(RewardSettlementWebMapper.toDetail(result));
    }
}
