package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.HobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface HobbyApiSpecV1 {

    @Operation(summary = "Hobby 목록 조회", description = "취미 목록(도감/선택용). category 필터 가능")
    ResponseEntity<ApiResponse<HobbyResponse.Infos>> hobbyInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    );
}
