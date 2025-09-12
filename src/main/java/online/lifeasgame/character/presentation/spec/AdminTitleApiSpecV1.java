package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.presentation.request.AdminTitleRequest;
import online.lifeasgame.character.presentation.response.AdminTitleResponse.TitleInfo;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminTitleApiSpecV1 {

    @Operation(summary = "Title 추가", description = "Title을 생성합니다")
    ResponseEntity<ApiResponse<TitleInfo>> create(
            @Valid @RequestBody AdminTitleRequest.CreateTitle request
    );
}
