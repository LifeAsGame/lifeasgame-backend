package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminCertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface AdminCertificationApiDraftSpecV1 {

    @Operation(summary = "Certification 목록 조회", description = "Certification 목록을 조회합니다. category/issuer 필터 가능")
    ResponseEntity<ApiResponse<AdminCertificationResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories,
            @RequestParam(name = "issuer", required = false) String issuer
    );

    @Operation(summary = "Certification 단건 조회", description = "Certification 단건을 조회합니다.")
    ResponseEntity<ApiResponse<AdminCertificationResponse.Info>> get(
            @PathVariable Long certificationId
    );

    @Operation(summary = "Certification 수정", description = "Certification을 수정합니다.")
    ResponseEntity<ApiResponse<AdminCertificationResponse.Info>> update(
            @PathVariable Long certificationId,
            @Valid @RequestBody AdminCertificationRequest.Update request
    );

    @Operation(summary = "Certification 삭제", description = "Certification을 삭제합니다.")
    ResponseEntity<ApiResponse<AdminCertificationResponse.Deleted>> delete(
            @PathVariable Long certificationId
    );
}
