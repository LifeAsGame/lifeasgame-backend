package online.lifeasgame.character.api.player;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.CertificationService;
import online.lifeasgame.character.application.result.CertificationResult.CertificationInfo;
import online.lifeasgame.character.api.player.mapper.CertificationWebMapper;
import online.lifeasgame.character.api.player.response.CertificationResponse;
import online.lifeasgame.character.api.player.spec.CertificationApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certifications")
public class CertificationController implements CertificationApiSpecV1 {

    private final CertificationService CertificationService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<CertificationResponse.CertificationInfos>> certificationInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<CertificationInfo> CertificationInfos = CertificationService.getCertifications(categories);
        return ApiResponses.ok(
                CertificationWebMapper.toCertificationInfos(CertificationInfos)
        );
    }
}
