package online.lifeasgame.home.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.home.api.response.HomeResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Home API V1 (Player)")
public interface HomeApiSpecV1 {

    @Operation(summary = "현재 Player world summary 조회")
    ResponseEntity<ApiResponse<HomeResponse.Summary>> home();
}
