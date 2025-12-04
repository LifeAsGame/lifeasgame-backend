package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerCertificationWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerCertificationApiSpecV1;
import online.lifeasgame.character.application.PlayerCertificationService;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerCertificationController implements AdminPlayerCertificationApiSpecV1 {

    private final PlayerCertificationService adminPlayerCertificationService;

    @Override
    @PostMapping("/{playerId}/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<AdminPlayerCertificationResponse.Granted>> grantCertification(
            @PathVariable Long playerId,
            @PathVariable Long certificationId,
            @Valid @RequestBody AdminPlayerCertificationRequest.Grant request
    ) {
        PlayerCertificationResult.Granted result = adminPlayerCertificationService.grantCertification(
                AdminPlayerCertificationWebMapper.toGrantCommand(playerId, certificationId, request)
        );

        return ApiResponses.ok(AdminPlayerCertificationWebMapper.toGranted(result));
    }
}
