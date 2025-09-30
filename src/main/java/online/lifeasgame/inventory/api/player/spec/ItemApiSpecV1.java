package online.lifeasgame.inventory.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.player.response.ItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Item API V1 (Player)")
public interface ItemApiSpecV1 {


    @Operation(summary = "아이템 상세 조회", description = "아이템 상세 정보를 조회합니다.")
    ResponseEntity<ApiResponse<ItemResponse.Detail>> getItems(@PathVariable Long itemId);

    @Operation(summary = "아이템 목록 조회", description = "필터/페이지로 아이템 카탈로그를 조회합니다.")
    ResponseEntity<ApiResponse<ItemResponse.Page<ItemResponse.Summary>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
