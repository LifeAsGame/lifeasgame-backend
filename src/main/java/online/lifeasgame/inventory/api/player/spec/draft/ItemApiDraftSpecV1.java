package online.lifeasgame.inventory.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.player.response.ItemResponse;
import org.springframework.http.ResponseEntity;

public interface ItemApiDraftSpecV1 {
    @Operation(summary = "아이템 메타 조회", description = "아이템 필터에 필요한 카테고리/타입/레어도 목록을 조회합니다.")
    ResponseEntity<ApiResponse<ItemResponse.Meta>> meta();
}
