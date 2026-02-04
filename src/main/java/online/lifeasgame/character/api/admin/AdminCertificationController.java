package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminCertificationWebMapper;
import online.lifeasgame.character.api.admin.request.AdminCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminCertificationResponse;
import online.lifeasgame.character.api.admin.spec.AdminCertificationApiSpecV1;
import online.lifeasgame.character.application.CertificationService;
import online.lifeasgame.character.application.result.CertificationResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/certifications")
public class AdminCertificationController implements AdminCertificationApiSpecV1 {

    private final CertificationService certificationService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminCertificationResponse.Info>> create(
            @Valid @RequestBody AdminCertificationRequest.Create request
    ) {
        CertificationResult.Info result = certificationService.create(AdminCertificationWebMapper.toCreateCommand(request));
        return ApiResponses.created(
                URI.create("/admin/v1/certifications/"),
                AdminCertificationWebMapper.toInfo(result)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminCertificationResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<CertificationResult.Info> results = certificationService.getCertifications(categories);
        return ApiResponses.ok(AdminCertificationWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/{certificationId}")
    public ResponseEntity<ApiResponse<AdminCertificationResponse.Info>> get(
            @PathVariable Long certificationId
    ) {
        CertificationResult.Info result = certificationService.getCertification(certificationId);
        return ApiResponses.ok(AdminCertificationWebMapper.toInfo(result));
    }
}
