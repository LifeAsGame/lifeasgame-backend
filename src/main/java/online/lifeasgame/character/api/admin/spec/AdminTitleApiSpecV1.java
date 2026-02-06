package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminTitleRequest;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse;
import online.lifeasgame.character.api.admin.response.AdminTitleResponse.Info;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Admin Title API V1")
public interface AdminTitleApiSpecV1 {

    @Operation(summary = "Title 생성", description = "Title을 생성합니다.")
    ResponseEntity<ApiResponse<Info>> create(
            @Valid @RequestBody AdminTitleRequest.Create request
    );

    @Operation(summary = "Title 목록 조회", description = "Title 목록을 조회합니다. category 필터 가능")
    ResponseEntity<ApiResponse<AdminTitleResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories
    );

    @Operation(summary = "Title 단건 조회", description = "Title 단건을 조회합니다.")
    ResponseEntity<ApiResponse<AdminTitleResponse.Info>> get(
            @PathVariable Long titleId
    );

    @Operation(summary = "Title 수정", description = "Title을 수정합니다.")
    ResponseEntity<ApiResponse<AdminTitleResponse.Info>> update(
            @PathVariable Long titleId,
            @Valid @RequestBody AdminTitleRequest.Update request
    );

    @Operation(summary = "Title 삭제", description = "Title을 삭제합니다.")
    ResponseEntity<ApiResponse<AdminTitleResponse.Deleted>> delete(
            @PathVariable Long titleId
    );
}
