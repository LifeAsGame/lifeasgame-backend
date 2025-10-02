package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminPlayerCertificationService;
import online.lifeasgame.character.application.result.AdminPlayerCertificationResult;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerCertificationWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerCertificationApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerCertificationController implements AdminPlayerCertificationApiSpecV1 {

    private final AdminPlayerCertificationService adminPlayerCertificationService;

    @Override
    @PostMapping("/{playerId}/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<AdminPlayerCertificationResponse.GrantedCertification>> grantCertification(
            @PathVariable Long playerId,
            @PathVariable Long certificationId,
            @Valid @RequestBody AdminPlayerCertificationRequest.GrantCertification request
    ) {
        AdminPlayerCertificationResult.GrantedCertification grantedCertification = adminPlayerCertificationService.grantCertification(
                AdminPlayerCertificationWebMapper.toCommand(playerId, certificationId, request)
        );

        return ApiResponses.ok(
                AdminPlayerCertificationWebMapper.toGrantedCertification(grantedCertification)
        );
    }
}
