package online.lifeasgame.social.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerConnectionWebMapper;
import online.lifeasgame.social.api.player.response.PlayerConnectionResponse;
import online.lifeasgame.social.api.player.spec.PlayerConnectionApiSpecV1;
import online.lifeasgame.social.application.ConnectionQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/connections")
public class PlayerConnectionController implements PlayerConnectionApiSpecV1 {

    private final ConnectionQueryService connectionQueryService;

    @Override
    @GetMapping("/followings")
    public ResponseEntity<ApiResponse<PlayerConnectionResponse.Page<PlayerConnectionResponse.Following>>> followings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponses.ok(PlayerConnectionWebMapper.toFollowingPage(
                connectionQueryService.followings(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100)
                )
        ));
    }

    @Override
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<PlayerConnectionResponse.Page<PlayerConnectionResponse.Follower>>> followers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponses.ok(PlayerConnectionWebMapper.toFollowerPage(
                connectionQueryService.followers(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100)
                )
        ));
    }
}
