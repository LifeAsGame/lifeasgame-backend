package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminPlayerCertificationApiDraftSpecV1 {
    @Operation(summary = "Player Certification 회수", description = "Player의 Certification을 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerCertificationResponse.Revoked>> revokeCertification(
            @PathVariable Long playerId,
            @PathVariable Long certificationId
    );
}
