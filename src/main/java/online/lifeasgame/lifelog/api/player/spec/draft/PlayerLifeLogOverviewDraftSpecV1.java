package online.lifeasgame.lifelog.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.response.PlayerLifeLogMetaResponse;
import online.lifeasgame.lifelog.api.player.response.PlayerLifeLogOverviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface PlayerLifeLogOverviewDraftSpecV1 {

    @Operation(summary = "라이프로그 대시보드", description = "텍스트 UI용: 최근 미디어/운동/컬렉션을 한 번에 조회합니다.")
    ResponseEntity<ApiResponse<PlayerLifeLogOverviewResponse.Dashboard>> dashboard(
            @RequestParam(defaultValue = "5") int mediaLimit,
            @RequestParam(defaultValue = "5") int exerciseLimit,
            @RequestParam(defaultValue = "5") int collectionLimit
    );

    @Operation(summary = "라이프로그 메타", description = "카테고리/상태 등 UI 선택지용 메타를 조회합니다.")
    ResponseEntity<ApiResponse<PlayerLifeLogMetaResponse.Meta>> meta();
}
