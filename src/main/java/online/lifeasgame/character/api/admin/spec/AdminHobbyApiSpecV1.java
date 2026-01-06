package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Hobby API V1")
public interface AdminHobbyApiSpecV1 {

    @Operation(summary = "Hobby 생성", description = "Hobby를 생성합니다.")
    ResponseEntity<ApiResponse<AdminHobbyResponse.Info>> create(
            @Valid @RequestBody AdminHobbyRequest.Create request
    );
}
