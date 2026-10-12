package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerFollowWebMapper;
import online.lifeasgame.social.api.player.request.PlayerFollowRequest;
import online.lifeasgame.social.api.player.response.PlayerFollowResponse;
import online.lifeasgame.social.api.player.spec.PlayerFollowApiSpecV1;
import online.lifeasgame.social.application.FollowService;
import online.lifeasgame.social.application.result.FollowResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/follows")
public class PlayerFollowController implements PlayerFollowApiSpecV1 {

    private final FollowService followService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @GetMapping("/followings")
    public ResponseEntity<ApiResponse<PlayerFollowResponse.Page<PlayerFollowResponse.Summary>>> followings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowResult.Page<FollowResult.Summary> result = followService.listFollowings(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        return ApiResponses.ok(PlayerFollowWebMapper.toSummaryPage(result));
    }

    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<PlayerFollowResponse.Page<PlayerFollowResponse.Summary>>> followers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowResult.Page<FollowResult.Summary> result = followService.listFollowers(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        return ApiResponses.ok(PlayerFollowWebMapper.toSummaryPage(result));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PlayerFollowResponse.Summary>>> recent(
            @RequestParam(defaultValue = "followings") String type,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<FollowResult.Summary> results = "followers".equalsIgnoreCase(type) ?
                followService.recentFollowers(playerId, Math.min(Math.max(limit, 1), 100)) :
                followService.recentFollowings(playerId, Math.min(Math.max(limit, 1), 100));

        return ApiResponses.ok(PlayerFollowWebMapper.toSummaries(results));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlayerFollowResponse.Info>> follow(
            @Valid @RequestBody PlayerFollowRequest.Create request
    ) {
        FollowResult.Info result = followService.follow(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                PlayerFollowWebMapper.toCreateCommand(request));
        return ApiResponses.ok(PlayerFollowWebMapper.toInfo(result));
    }

    @PostMapping("/{followId}/unfollow")
    public ResponseEntity<ApiResponse<Void>> unfollow(@PathVariable Long followId) {
        followService.unfollow(currentPlayerAccessor.currentPlayerIdOrThrow(), followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/mute")
    public ResponseEntity<ApiResponse<Void>> mute(@PathVariable Long followId) {
        followService.mute(currentPlayerAccessor.currentPlayerIdOrThrow(), followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/unmute")
    public ResponseEntity<ApiResponse<Void>> unmute(@PathVariable Long followId) {
        followService.unmute(currentPlayerAccessor.currentPlayerIdOrThrow(), followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/block")
    public ResponseEntity<ApiResponse<Void>> block(@PathVariable Long followId) {
        followService.block(currentPlayerAccessor.currentPlayerIdOrThrow(), followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblock(@PathVariable Long followId) {
        followService.unblock(currentPlayerAccessor.currentPlayerIdOrThrow(), followId);
        return ApiResponses.ok(null);
    }
}
