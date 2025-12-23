package online.lifeasgame.character.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.CertificationWebMapper;
import online.lifeasgame.character.api.player.response.CertificationResponse;
import online.lifeasgame.character.api.player.spec.CertificationApiSpecV1;
import online.lifeasgame.character.application.CertificationService;
import online.lifeasgame.character.application.result.CertificationResult.Info;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certifications")
public class CertificationController implements CertificationApiSpecV1 {

    private final CertificationService certificationservice;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<CertificationResponse.Infos>> certificationInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<Info> results = certificationservice.getCertifications(categories);
        return ApiResponses.ok(CertificationWebMapper.toInfos(results));
    }
}
