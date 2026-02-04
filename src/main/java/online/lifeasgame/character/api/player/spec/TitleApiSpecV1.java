package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.TitleResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface TitleApiSpecV1 {

    @Operation(summary = "Title 목록 조회", description = "Title 목록을 조회합니다. category 필터 가능")
    ResponseEntity<ApiResponse<TitleResponse.Infos>> titleInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    );

    @Operation(summary = "Title 단건 조회", description = "Title 상세(텍스트 UI 상세보기용)")
    ResponseEntity<ApiResponse<TitleResponse.Info>> titleInfo(
            @PathVariable Long titleId
    );
}
