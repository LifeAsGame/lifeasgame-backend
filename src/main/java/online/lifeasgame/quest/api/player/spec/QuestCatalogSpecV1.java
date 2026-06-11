package online.lifeasgame.quest.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Quest API V1")
public interface QuestCatalogSpecV1 {

    @Operation(summary = "Quest 카탈로그 조회", description = "Quest Blueprint(게시판) 목록을 조회합니다. (필터 선택)")
    ResponseEntity<QuestResponse.Blueprints> catalog(
//            @RequestParam(name = "category", required = false) List<String> categories,
//            @RequestParam(name = "repeatRule", required = false) List<String> repeatRules
    );
}
