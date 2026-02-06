package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminTitleApiDraftSpecV1 {

    @Operation(summary = "Title 삭제", description = "Title을 삭제합니다.")
    ResponseEntity<ApiResponse<AdminTitleResponse.Deleted>> delete(
            @PathVariable Long titleId
    );
}
