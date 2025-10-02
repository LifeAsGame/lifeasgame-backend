package online.lifeasgame.character.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.response.AdminPlayerTitleResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminPlayerTitleApiSpecV1 {

    @Operation(summary = "Player Title 지급", description = "Player에게 Title을 지급합니다.")
    ResponseEntity<ApiResponse<AdminPlayerTitleResponse.GrantedTitle>> grantTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    );
}
