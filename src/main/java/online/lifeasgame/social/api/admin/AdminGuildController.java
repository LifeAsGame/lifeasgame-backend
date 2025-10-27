package online.lifeasgame.social.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.admin.mapper.AdminGuildWebMapper;
import online.lifeasgame.social.api.admin.request.AdminGuildRequest;
import online.lifeasgame.social.api.admin.response.AdminGuildResponse;
import online.lifeasgame.social.api.admin.spec.AdminGuildApiSpecV1;
import online.lifeasgame.social.application.GuildService;
import online.lifeasgame.social.application.result.GuildResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1")
public class AdminGuildController implements AdminGuildApiSpecV1 {

    private final GuildService guildService;

    @Override
    @GetMapping("/players/{playerId}/guilds/{guildId}")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Summary>> getGuildInfo(
            @PathVariable Long playerId,
            @PathVariable Long guildId
    ) {
        var info = guildService.getGuild(playerId, guildId);
        return ApiResponses.ok(AdminGuildWebMapper.toSummary(info));
    }

    // ===== 일반 조회 =====
    @Override
    @GetMapping("/guilds/search")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Page<AdminGuildResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        GuildResult.Page<GuildResult.Summary> pages = guildService.search(
                keyword,
                visibility,
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        return ApiResponses.ok(AdminGuildWebMapper.toSummaryPage(pages));
    }

    @Override
    @GetMapping("/guilds/recent")
    public ResponseEntity<ApiResponse<List<AdminGuildResponse.Summary>>> recent(
            @RequestParam(defaultValue="10") Integer limit
    ) {
        var infos = guildService.recent(Math.min(Math.max(limit, 1), 100));
        return ApiResponses.ok(AdminGuildWebMapper.toSummaries(infos));
    }

    // ===== 플레이어 스코프(acting as playerId) =====

    // 생성
    @Override
    @PostMapping("/players/{playerId}/guilds")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminGuildRequest.Create body
    ) {
        var info = guildService.create(playerId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(AdminGuildWebMapper.toDetail(info));
    }

    // 기본 변경
    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/rename")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> rename(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Rename body
    ) {
        var info = guildService.rename(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(AdminGuildWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/policy")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> changePolicy(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.ChangePolicy body
    ) {
        var info = guildService.changePolicy(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(AdminGuildWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/description")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> changeDescription(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.ChangeDescription body
    ) {
        var info = guildService.changeDescription(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(AdminGuildWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/emblem")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> changeEmblem(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.ChangeEmblem body
    ) {
        var info = guildService.changeEmblem(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(AdminGuildWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/tags/add")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> addTag(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.TagOp body
    ) {
        var info = guildService.addTag(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(AdminGuildWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/tags/remove")
    public ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> removeTag(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.TagOp body
    ) {
        var info = guildService.removeTag(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(AdminGuildWebMapper.toDetail(info));
    }

    // 가입/권한
    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.RequestJoin body
    ) {
        guildService.requestJoin(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Approve body
    ) {
        guildService.approveJoin(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Reject body
    ) {
        guildService.rejectJoin(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long playerId, @PathVariable Long guildId) {
        guildService.cancelJoin(playerId, guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/transfer-leader")
    public ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.TransferLeader body
    ) {
        guildService.transferLeader(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Kick body
    ) {
        guildService.kick(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.MemberOp body
    ) {
        guildService.promote(playerId, guildId, AdminGuildWebMapper.toCommandPromote(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/demote")
    public ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.MemberOp body
    ) {
        guildService.demote(playerId, guildId, AdminGuildWebMapper.toCommandDemote(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long playerId, @PathVariable Long guildId) {
        guildService.leave(playerId, guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/disband")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long playerId, @PathVariable Long guildId) {
        guildService.disbandByLeader(playerId, guildId);
        return ApiResponses.ok(null);
    }

    // 초대
    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Invite body
    ) {
        guildService.invite(playerId, guildId, AdminGuildWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/accept-invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long playerId, @PathVariable Long guildId) {
        guildService.acceptInvitation(playerId, guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/guilds/{guildId}/decline-invitation")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(
            @PathVariable Long playerId,
            @PathVariable Long guildId
    ) {
        guildService.declineInvitation(playerId, guildId);
        return ApiResponses.ok(null);
    }
}
