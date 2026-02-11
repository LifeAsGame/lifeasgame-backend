package online.lifeasgame.social.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.response.PlayerFollowResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface PlayerFollowApiDraftSpecV1 {

    @Operation(summary = "팔로우 관계 조회(내 기준)")
    ResponseEntity<ApiResponse<PlayerFollowResponse.Relationship>> relationship(
            @RequestParam Long targetPlayerId
    );
}
