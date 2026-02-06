package online.lifeasgame.lifelog.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerMediaLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "LifeLog Media API V1 (Player)")
public interface PlayerMediaLogSpecV1 {

    @Operation(summary = "최근 항목 조회(플레이어)")
    ResponseEntity<List<PlayerMediaLogResponse.Info>> recent(Integer limit);

    @Operation(summary = "검색(플레이어)", description = "page/size 기반 검색. UI 페이징을 위해 Page 응답을 권장합니다.")
    ResponseEntity<List<PlayerMediaLogResponse.Info>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "미디어 로그 생성(플레이어)")
    ResponseEntity<PlayerMediaLogResponse.Created> create(PlayerMediaLogRequest.Create request);

    @Operation(summary = "평점 반영(플레이어)")
    ResponseEntity<PlayerMediaLogResponse.Info> rate(Long mediaId, PlayerMediaLogRequest.Rate request);

    @Operation(summary = "에피소드 진도 증가(플레이어)")
    ResponseEntity<PlayerMediaLogResponse.Info> advance(Long mediaId, PlayerMediaLogRequest.Advance request);

    @Operation(summary = "상태 변경(플레이어)")
    ResponseEntity<PlayerMediaLogResponse.Info> markStatus(Long mediaId, PlayerMediaLogRequest.MarkStatus request);

    @Operation(summary = "리와치(+1)(플레이어)")
    ResponseEntity<PlayerMediaLogResponse.Info> rewatch(@PathVariable Long mediaId);

    @Operation(summary = "미디어 로그 수정(플레이어)", description = "제목/카테고리/태그/진도/상태 등 편집용")
    ResponseEntity<ApiResponse<PlayerMediaLogResponse.Info>> update(
            @PathVariable Long mediaId,
            @Valid @RequestBody PlayerMediaLogRequest.Update request
    );

}
