package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse.Info;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Title API V1")
public interface AdminTitleApiSpecV1 {

    @Operation(summary = "Title 생성", description = "Title을 생성합니다.")
    ResponseEntity<ApiResponse<Info>> create(
            @Valid @RequestBody AdminTitleRequest.Create request
    );
}
