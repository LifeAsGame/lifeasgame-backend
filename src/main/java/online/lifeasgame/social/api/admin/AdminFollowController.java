package online.lifeasgame.social.api.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.admin.mapper.AdminFollowWebMapper;
import online.lifeasgame.social.api.admin.request.AdminFollowRequest;
import online.lifeasgame.social.api.admin.response.AdminFollowResponse;
import online.lifeasgame.social.api.admin.spec.AdminFollowApiSpecV1;
import online.lifeasgame.social.application.FollowService;
import online.lifeasgame.social.application.result.FollowResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players/{playerId}/follows")
public class AdminFollowController implements AdminFollowApiSpecV1 {

    private final FollowService followService;

    // 조회
    @GetMapping("/followings")
    public ResponseEntity<ApiResponse<AdminFollowResponse.Page<AdminFollowResponse.Summary>>> followings(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowResult.Page<FollowResult.Summary> pages = followService.listFollowings(
                playerId,
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        return ApiResponses.ok(AdminFollowWebMapper.toSummaryPage(pages));
    }

    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<AdminFollowResponse.Page<AdminFollowResponse.Summary>>> followers(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FollowResult.Page<FollowResult.Summary> pages = followService.listFollowers(
                playerId,
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        return ApiResponses.ok(AdminFollowWebMapper.toSummaryPage(pages));
    }

    // 조작
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> follow(
            @PathVariable Long playerId,
            @RequestBody AdminFollowRequest.Create request
    ) {
        followService.follow(playerId, AdminFollowWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/unfollow")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable Long playerId,
            @PathVariable Long followId
    ) {
        followService.unfollow(playerId, followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/mute")
    public ResponseEntity<ApiResponse<Void>> mute(
            @PathVariable Long playerId,
            @PathVariable Long followId
    ) {
        followService.mute(playerId, followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/unmute")
    public ResponseEntity<ApiResponse<Void>> unmute(
            @PathVariable Long playerId,
            @PathVariable Long followId
    ) {
        followService.unmute(playerId, followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/block")
    public ResponseEntity<ApiResponse<Void>> block(
            @PathVariable Long playerId,
            @PathVariable Long followId
    ) {
        followService.block(playerId, followId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{followId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblock(
            @PathVariable Long playerId,
            @PathVariable Long followId
    ) {
        followService.unblock(playerId, followId);
        return ApiResponses.ok(null);
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AdminFollowResponse.Summary>>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "followings") String type,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        List<FollowResult.Summary> infos = "followers".equalsIgnoreCase(type)
                ? followService.recentFollowers(playerId, limit) : followService.recentFollowings(playerId, limit);
        return ApiResponses.ok(AdminFollowWebMapper.toSummaries(infos));
    }
}
