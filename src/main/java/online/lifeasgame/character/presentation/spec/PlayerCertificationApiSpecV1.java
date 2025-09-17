package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.presentation.request.PlayerCertificationRequest;
import online.lifeasgame.character.presentation.response.PlayerCertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlayerCertificationApiSpecV1 {

    @Operation(summary = "Player 보유 자격증 목록 출력", description = "사용자가 보유한 자격증 목록을 출력합니다")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.PlayerCertificationInfos>> playerCertificationInfos();

    @Operation(summary = "Player 자격증 변경", description = "사용자의 자격증 정보를 변경합니다")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.ChangedPlayerCertification>> updatePlayerCertification(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.ChangePlayerCertification request
    );
}
