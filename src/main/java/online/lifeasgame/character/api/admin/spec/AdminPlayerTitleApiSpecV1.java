package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Admin Player Title API V1")
public interface AdminPlayerTitleApiSpecV1 {

    @Operation(summary = "Player Title 조회", description = "Player의 Title holder summary를 조회합니다.")
    ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Infos>> getTitles(
            @PathVariable @Positive Long playerId
    );

    @Operation(summary = "Player Title 지급", description = "Player에게 Title을 지급합니다.")
    ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Granted>> grantTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    );

    @Operation(summary = "Player Title 회수", description = "Player의 Title을 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Revoked>> revokeTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    );
}
