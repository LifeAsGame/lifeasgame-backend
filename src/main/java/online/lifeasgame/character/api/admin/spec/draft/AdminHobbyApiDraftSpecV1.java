package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminHobbyApiDraftSpecV1 {

    @Operation(summary = "Hobby 수정", description = "Hobby를 수정합니다.")
    ResponseEntity<ApiResponse<AdminHobbyResponse.Info>> update(
            @PathVariable Long hobbyId,
            @Valid @RequestBody AdminHobbyRequest.Update request
    );

    @Operation(summary = "Hobby 삭제", description = "Hobby를 삭제합니다.")
    ResponseEntity<ApiResponse<AdminHobbyResponse.Deleted>> delete(
            @PathVariable Long hobbyId
    );
}
