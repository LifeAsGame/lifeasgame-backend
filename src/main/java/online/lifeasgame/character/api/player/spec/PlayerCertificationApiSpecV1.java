package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.player.request.PlayerCertificationRequest;
import online.lifeasgame.character.api.player.response.PlayerCertificationResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Player Certification API V1")
public interface PlayerCertificationApiSpecV1 {

    @Operation(summary = "Player 보유 자격증 목록", description = "사용자가 보유한 자격증 목록을 출력합니다.")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.Infos>> playerCertificationInfos();

    @Operation(summary = "자격증 추가", description = "사용자가 자격증을 추가합니다.")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.Created>> create(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.Create request
    );

    @Operation(summary = "자격증 수정", description = "사용자가 자격증을 수정합니다.")
    ResponseEntity<ApiResponse<PlayerCertificationResponse.Changed>> update(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.Update request
    );

    @Operation(summary = "자격증 삭제", description = "사용자가 자격증을 삭제합니다.")
    ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable Long certificationId
    );
}
