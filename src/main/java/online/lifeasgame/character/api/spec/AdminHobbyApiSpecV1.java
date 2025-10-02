package online.lifeasgame.character.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.request.AdminHobbyRequest;
import online.lifeasgame.character.api.response.AdminHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminHobbyApiSpecV1 {

    @Operation(summary = "Hobby 추가", description = "Hobby을 생성합니다")
    ResponseEntity<ApiResponse<AdminHobbyResponse.HobbyInfo>> create(
            @Valid @RequestBody AdminHobbyRequest.CreateHobby request
    );
}
