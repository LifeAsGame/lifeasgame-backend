package online.lifeasgame.quest.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Player Quest API V1")
public interface QuestSpecV1 {

    @Operation(summary = "내 퀘스트 목록", description = "플레이어의 Quest Acceptance 목록을 조회합니다. (상태/카테고리 필터 가능)")
    ResponseEntity<QuestResponse.Acceptances> list(
            @RequestParam(required = false) String status
//                        @RequestParam(name = "status", required = false) List<String> statuses,
//            @RequestParam(name = "category", required = false) List<String> categories

    );

    @Operation(summary = "내 퀘스트 상세", description = "Quest 정의 + 플레이어의 최신 Acceptance(있으면)까지 함께 조회합니다.")
    ResponseEntity<QuestResponse.PlayerQuest> detail(
            @PathVariable String questCode
    );
}
