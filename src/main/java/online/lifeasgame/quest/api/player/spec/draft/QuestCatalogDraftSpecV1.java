package online.lifeasgame.quest.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import org.springframework.http.ResponseEntity;

public interface QuestCatalogDraftSpecV1 {

    @Operation(summary = "Quest 메타 조회", description = "프론트 필터/표시에 필요한 enum 메타(카테고리/타겟/반복/상태)를 반환합니다.")
    ResponseEntity<ApiResponse<QuestResponse.Meta>> meta();
}
