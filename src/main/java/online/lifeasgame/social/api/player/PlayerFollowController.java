package online.lifeasgame.social.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerFollowWebMapper;
import online.lifeasgame.social.api.player.request.PlayerFollowRequest;
import online.lifeasgame.social.api.player.response.PlayerFollowResponse;
import online.lifeasgame.social.api.player.spec.PlayerFollowApiSpecV1;
import online.lifeasgame.social.application.FollowFacade;
import online.lifeasgame.social.application.result.FollowResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/follows")
public class PlayerFollowController implements PlayerFollowApiSpecV1 {

    private final FollowFacade followFacade;

    // 생성
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerFollowResponse.Info>> follow(
            @RequestBody PlayerFollowRequest.Create request
    ) {
        FollowResult.Info info = followFacade.follow(PlayerFollowWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerFollowWebMapper.toInfo(info));
    }

    // 상태 변경
    @PostMapping("/{followId}/unfollow")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable Long followId
    ) {
        followFacade.unfollow(followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/mute")
    public ResponseEntity<ApiResponse<Void>> mute(
            @PathVariable Long followId
    ) {
        followFacade.mute(followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/unmute")
    public ResponseEntity<ApiResponse<Void>> unmute(
            @PathVariable Long followId
    ) {
        followFacade.unmute(followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/block")
    public ResponseEntity<ApiResponse<Void>> block(
            @PathVariable Long followId
    ) {
        followFacade.block(followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblock(
            @PathVariable Long followId
    ) {
        followFacade.unblock(followId);
        return ApiResponses.ok(null);
    }

    // 조회
    @GetMapping("/followings")
    public ResponseEntity<ApiResponse<PlayerFollowResponse.Page<PlayerFollowResponse.Summary>>> followings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowResult.Page<FollowResult.Summary> pages = followFacade.listFollowings(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        return ApiResponses.ok(PlayerFollowWebMapper.toSummaryPage(pages));
    }

    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<PlayerFollowResponse.Page<PlayerFollowResponse.Summary>>> followers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowResult.Page<FollowResult.Summary> pages = followFacade.listFollowers(

                Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ApiResponses.ok(PlayerFollowWebMapper.toSummaryPage(pages));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PlayerFollowResponse.Summary>>> recent(
            @RequestParam(defaultValue = "followings") String type,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<FollowResult.Summary> infos = "followers".equalsIgnoreCase(type) ? followFacade.recentFollowers(

                Math.min(Math.max(limit, 1), 100)) : followFacade.recentFollowings(Math.min(Math.max(limit, 1), 100));
        return ApiResponses.ok(PlayerFollowWebMapper.toSummaries(infos));
    }
}

