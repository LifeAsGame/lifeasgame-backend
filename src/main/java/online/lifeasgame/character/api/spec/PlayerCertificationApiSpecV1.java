package online.lifeasgame.character.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.request.PlayerCertificationRequest;
import online.lifeasgame.character.api.response.PlayerCertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlayerCertificationApiSpecV1 {

    @Operation(summary = "Player 보유 자격증 목록 출력", description = "사용자가 보유한 자격증 목록을 출력합니다")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.PlayerCertificationInfos>> playerCertificationInfos();

    @Operation(summary = "Player Certification 생성", description = "Player에게 Certification를 생성합니다")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.CreatedPlayerCertification>> createPlayerCertification(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.CreatePlayerCertification request
    );

    @Operation(summary = "Player 자격증 변경", description = "사용자의 자격증 정보를 변경합니다")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.ChangedPlayerCertification>> updatePlayerCertification(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.ChangePlayerCertification request
    );

    @Operation(summary = "Player 자격증 삭제", description = "사용자의 자격증을 제거합니다")
    ResponseEntity<ApiResponse<Long>> deletePlayerCertification(@PathVariable Long certificationId);
}
