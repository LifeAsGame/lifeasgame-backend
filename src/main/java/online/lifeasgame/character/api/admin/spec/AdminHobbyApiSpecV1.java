package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Admin Hobby API V1")
public interface AdminHobbyApiSpecV1 {

    @Operation(summary = "Hobby 생성", description = "Hobby를 생성합니다.")
    ResponseEntity<ApiResponse<AdminHobbyResponse.Info>> create(
            @Valid @RequestBody AdminHobbyRequest.Create request
    );

    @Operation(summary = "Hobby 목록 조회", description = "Hobby 목록을 조회합니다. category 필터 가능")
    ResponseEntity<ApiResponse<AdminHobbyResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories
    );

    @Operation(summary = "Hobby 단건 조회", description = "Hobby 단건을 조회합니다.")
    ResponseEntity<ApiResponse<AdminHobbyResponse.Info>> get(
            @PathVariable Long hobbyId
    );

}
