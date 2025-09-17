package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.presentation.request.AdminCertificationRequest;
import online.lifeasgame.character.presentation.response.AdminCertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminCertificationApiSpecV1 {

    @Operation(summary = "Certification 추가", description = "Certification을 생성합니다")
    ResponseEntity<ApiResponse<AdminCertificationResponse.CertificationInfo>> create(
            @Valid @RequestBody AdminCertificationRequest.CreateCertification request
    );
}
