package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Player Certification API V1")
public interface AdminPlayerCertificationApiSpecV1 {

    @Operation(summary = "Player Certification 지급", description = "Player에게 Certification를 지급합니다")
    ResponseEntity<ApiResponse<AdminPlayerCertificationResponse.Granted>> grantCertification(
            @PathVariable Long playerId,
            @PathVariable Long certificationId,
            @Valid @RequestBody AdminPlayerCertificationRequest.Create request
    );
}
