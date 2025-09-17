package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import online.lifeasgame.character.presentation.response.CertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface CertificationApiSpecV1 {

    @Operation(summary = "Certification 목록 조회", description = "Certification 목록 조회: category 설정 가능")
    ResponseEntity<ApiResponse<CertificationResponse.CertificationInfos>> certificationInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    );
}
