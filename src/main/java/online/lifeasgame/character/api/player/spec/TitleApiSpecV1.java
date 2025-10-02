package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import online.lifeasgame.character.api.player.response.TitleResponse.Infos;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface TitleApiSpecV1 {

    @Operation(summary = "Title 목록 조회", description = "Title 목록 조회: category 설정 가능")
    ResponseEntity<ApiResponse<Infos>> titleInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    );
}
