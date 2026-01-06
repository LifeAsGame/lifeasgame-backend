package online.lifeasgame.character.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.CertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface CertificationApiDraftSpecV1 {

    @Operation(summary = "Certification 단건 조회", description = "자격증 상세(텍스트 UI 상세보기용)")
    ResponseEntity<ApiResponse<CertificationResponse.Info>> certificationInfo(
            @PathVariable Long certificationId
    );
}
