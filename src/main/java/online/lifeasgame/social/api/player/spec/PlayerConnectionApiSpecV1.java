package online.lifeasgame.social.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.response.PlayerConnectionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Social Connections API V1 (Player)")
public interface PlayerConnectionApiSpecV1 {

    @Operation(summary = "현재 Player의 팔로잉 연결 목록")
    ResponseEntity<ApiResponse<PlayerConnectionResponse.Page<PlayerConnectionResponse.Following>>> followings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "현재 Player의 팔로워 연결 목록")
    ResponseEntity<ApiResponse<PlayerConnectionResponse.Page<PlayerConnectionResponse.Follower>>> followers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
