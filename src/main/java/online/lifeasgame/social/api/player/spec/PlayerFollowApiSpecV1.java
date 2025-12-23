package online.lifeasgame.social.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.request.PlayerFollowRequest;
import online.lifeasgame.social.api.player.response.PlayerFollowResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Social Follow API V1 (Player)")
public interface PlayerFollowApiSpecV1 {

    @Operation(summary = "내가 팔로우하는 목록")
    ResponseEntity<ApiResponse<PlayerFollowResponse.Page<PlayerFollowResponse.Summary>>> followings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(
                    defaultValue = "20"
            ) int size
    );

    @Operation(summary = "나를 팔로우하는 목록")
    ResponseEntity<ApiResponse<PlayerFollowResponse.Page<PlayerFollowResponse.Summary>>> followers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(
                    defaultValue = "20"
            ) int size
    );

    @Operation(summary = "최근 팔로우(내가 팔로우 / 나를 팔로우)")
    ResponseEntity<ApiResponse<List<PlayerFollowResponse.Summary>>> recent(
            @RequestParam(defaultValue = "followings") String type,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    );

    @Operation(summary = "팔로우 생성")
    ResponseEntity<ApiResponse<PlayerFollowResponse.Info>> follow(
            @Valid @RequestBody PlayerFollowRequest.Create request
    );

    @Operation(summary = "언팔로우")
    ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable Long followId
    );

    @Operation(summary = "뮤트")
    ResponseEntity<ApiResponse<Void>> mute(
            @PathVariable Long followId
    );

    @Operation(summary = "뮤트 해제")
    ResponseEntity<ApiResponse<Void>> unmute(
            @PathVariable Long followId
    );

    @Operation(summary = "차단")
    ResponseEntity<ApiResponse<Void>> block(
            @PathVariable Long followId
    );

    @Operation(summary = "차단 해제")
    ResponseEntity<ApiResponse<Void>> unblock(
            @PathVariable Long followId
    );
}

