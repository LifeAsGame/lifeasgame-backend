package online.lifeasgame.social.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerGuildWebMapper;
import online.lifeasgame.social.api.player.request.PlayerGuildRequest;
import online.lifeasgame.social.api.player.response.PlayerGuildResponse;
import online.lifeasgame.social.api.player.spec.PlayerGuildApiSpecV1;
import online.lifeasgame.social.application.GuildFacade;
import online.lifeasgame.social.application.result.GuildResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guilds")
public class PlayerGuildController implements PlayerGuildApiSpecV1 {

    private final GuildFacade guildFacade;

    // 생성/기본 변경
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> create(
            @RequestBody PlayerGuildRequest.Create request
    ) {
        GuildResult.Info result = guildFacade.create(PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @PostMapping("/{guildId}/rename")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> rename(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.Rename request
    ) {
        GuildResult.Info result = guildFacade.rename(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @PostMapping("/{guildId}/policy")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changePolicy(
            @PathVariable Long guildId,
            @RequestBody
            PlayerGuildRequest.ChangePolicy request
    ) {
        GuildResult.Info result = guildFacade.changePolicy(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @PostMapping("/{guildId}/description")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeDescription(
            @PathVariable Long guildId,
            @RequestBody
            PlayerGuildRequest.ChangeDescription request
    ) {
        GuildResult.Info result = guildFacade.changeDescription(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @PostMapping("/{guildId}/emblem")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeEmblem(
            @PathVariable Long guildId,
            @RequestBody
            PlayerGuildRequest.ChangeEmblem request
    ) {
        GuildResult.Info result = guildFacade.changeEmblem(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @PostMapping("/{guildId}/tags/add")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> addTag(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.TagOp request
    ) {
        GuildResult.Info result = guildFacade.addTag(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @PostMapping("/{guildId}/tags/remove")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> removeTag(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.TagOp request
    ) {
        GuildResult.Info result = guildFacade.removeTag(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    // 가입/권한
    @PostMapping("/{guildId}/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.RequestJoin request
    ) {
        guildFacade.requestJoin(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveJoin(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.Approve request
    ) {
        guildFacade.approveJoin(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.Reject request
    ) {
        guildFacade.rejectJoin(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long guildId) {
        guildFacade.cancelJoin(guildId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/transfer-leader")
    public ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.TransferLeader request
    ) {
        guildFacade.transferLeader(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.Kick request
    ) {
        guildFacade.kick(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.MemberOp request
    ) {
        guildFacade.promote(guildId, PlayerGuildWebMapper.toCommandPromote(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/demote")
    public ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.MemberOp request
    ) {
        guildFacade.demote(guildId, PlayerGuildWebMapper.toCommandDemote(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long guildId) {
        guildFacade.leave(guildId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/disband")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long guildId) {
        guildFacade.disband(guildId);
        return ApiResponses.ok(null);
    }

    // 초대
    @PostMapping("/{guildId}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long guildId,
            @RequestBody PlayerGuildRequest.Invite request
    ) {
        guildFacade.invite(guildId, PlayerGuildWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/accept-invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long guildId) {
        guildFacade.acceptInvitation(guildId);
        return ApiResponses.ok(null);
    }

    @PostMapping("/{guildId}/decline-invitation")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long guildId) {
        guildFacade.declineInvitation(guildId);
        return ApiResponses.ok(null);
    }

    // 조회
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PlayerGuildResponse.Summary>>> recent(
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        var infos = guildFacade.recent(Math.min(Math.max(limit, 1), 100));
        return ApiResponses.ok(PlayerGuildWebMapper.toSummaryList(infos));
    }

    @GetMapping("/{guildId}")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Summary>> getGuildInfo(
            @PathVariable Long guildId
    ) {
        var info = guildFacade.getGuild(guildId);
        return ApiResponses.ok(PlayerGuildWebMapper.toSummary(info));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        GuildResult.Page<GuildResult.Summary> pages = guildFacade.search(
                keyword,
                visibility,
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        return ApiResponses.ok(PlayerGuildWebMapper.toSummaryPage(pages));
    }
}
