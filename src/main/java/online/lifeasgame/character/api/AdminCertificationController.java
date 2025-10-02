package online.lifeasgame.character.api;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminCertificationService;
import online.lifeasgame.character.application.result.AdminCertificationResult;
import online.lifeasgame.character.api.mapper.AdminCertificationWebMapper;
import online.lifeasgame.character.api.request.AdminCertificationRequest;
import online.lifeasgame.character.api.response.AdminCertificationResponse;
import online.lifeasgame.character.api.spec.AdminCertificationApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/certifications")
public class AdminCertificationController implements AdminCertificationApiSpecV1 {

    private final AdminCertificationService adminCertificationService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminCertificationResponse.CertificationInfo>> create(
            @Valid @RequestBody AdminCertificationRequest.CreateCertification request
    ) {
        AdminCertificationResult.CertificationInfo CertificationInfo = adminCertificationService.create(AdminCertificationWebMapper.toCommand(request));
        return ApiResponses.created(
                URI.create("/admin/v1/certifications/"),
                AdminCertificationWebMapper.toCertificationInfo(CertificationInfo)
        );
    }
}
