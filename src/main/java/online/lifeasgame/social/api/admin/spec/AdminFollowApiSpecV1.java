package online.lifeasgame.social.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.admin.request.AdminFollowRequest;
import online.lifeasgame.social.api.admin.response.AdminFollowResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Social Follow API V1 (Admin)")
public interface AdminFollowApiSpecV1 {

    @Operation(summary = "플레이어의 팔로잉 목록 (Admin)")
    ResponseEntity<ApiResponse<AdminFollowResponse.Page<AdminFollowResponse.Summary>>> followings(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "플레이어의 팔로워 목록 (Admin)")
    ResponseEntity<ApiResponse<AdminFollowResponse.Page<AdminFollowResponse.Summary>>> followers(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "플레이어로 팔로우 생성 (Admin)")
    ResponseEntity<ApiResponse<Void>> follow(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminFollowRequest.Create request
    );

    @Operation(summary = "플레이어로 언팔로우 (Admin)")
    ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable Long playerId,
            @PathVariable Long followId
    );

    @Operation(summary = "플레이어로 뮤트 (Admin)")
    ResponseEntity<ApiResponse<Void>> mute(
            @PathVariable Long playerId,
            @PathVariable Long followId
    );

    @Operation(summary = "플레이어로 뮤트 해제 (Admin)")
    ResponseEntity<ApiResponse<Void>> unmute(
            @PathVariable Long playerId,
            @PathVariable Long followId
    );

    @Operation(summary = "플레이어로 차단 (Admin)")
    ResponseEntity<ApiResponse<Void>> block(
            @PathVariable Long playerId,
            @PathVariable Long followId
    );

    @Operation(summary = "플레이어로 차단 해제 (Admin)")
    ResponseEntity<ApiResponse<Void>> unblock(
            @PathVariable Long playerId,
            @PathVariable Long followId
    );
}

