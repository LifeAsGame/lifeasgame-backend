package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.CertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface CertificationApiSpecV1 {

    @Operation(summary = "Certification 목록 조회", description = "자격증 목록(도감/선택용). category 필터 가능")
    ResponseEntity<ApiResponse<CertificationResponse.Infos>> certificationInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    );
}
