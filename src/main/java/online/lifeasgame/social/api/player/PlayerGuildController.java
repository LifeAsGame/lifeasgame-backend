package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Override
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PlayerGuildResponse.Summary>>> recent(
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<GuildResult.Summary> results = guildFacade.recent(Math.min(Math.max(limit, 1), 100));
        return ApiResponses.ok(PlayerGuildWebMapper.toSummaries(results));
    }

    @Override
    @GetMapping("/{guildId}")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> getGuildInfo(
            @PathVariable Long guildId
    ) {
        GuildResult.Info result = guildFacade.getGuild(guildId);
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        GuildResult.Page<GuildResult.Summary> result = guildFacade.search(keyword, visibility, page, size);
        return ApiResponses.ok(PlayerGuildWebMapper.toSummaryPage(result));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> create(
            @Valid @RequestBody PlayerGuildRequest.Create request
    ) {
        GuildResult.Info result = guildFacade.create(PlayerGuildWebMapper.toCreateCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/rename")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> rename(
            @PathVariable Long guildId,
            @Valid  @RequestBody PlayerGuildRequest.Rename request
    ) {
        GuildResult.Info result = guildFacade.rename(guildId, PlayerGuildWebMapper.toRenameCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/policy")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changePolicy(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.ChangePolicy request
    ) {
        GuildResult.Info result = guildFacade.changePolicy(guildId, PlayerGuildWebMapper.toChangePolicyCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/description")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeDescription(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.ChangeDescription request
    ) {
        GuildResult.Info result = guildFacade.changeDescription(guildId, PlayerGuildWebMapper.toChangeDescriptionCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/emblem")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeEmblem(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.ChangeEmblem request
    ) {
        GuildResult.Info result = guildFacade.changeEmblem(guildId, PlayerGuildWebMapper.toChangeEmblemCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/tags/add")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> addTag(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TagOp request
    ) {
        GuildResult.Info result = guildFacade.addTag(guildId, PlayerGuildWebMapper.toTagOpCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/tags/remove")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> removeTag(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TagOp request
    ) {
        GuildResult.Info result = guildFacade.removeTag(guildId, PlayerGuildWebMapper.toTagOpCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.RequestJoin request
    ) {
        guildFacade.requestJoin(guildId, PlayerGuildWebMapper.toRequestJoinCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long guildId) {
        guildFacade.cancelJoin(guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Approve request
    ) {
        guildFacade.approveJoin(guildId, PlayerGuildWebMapper.toApproveCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Reject request
    ) {
        guildFacade.rejectJoin(guildId, PlayerGuildWebMapper.toRejectCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Invite request
    ) {
        guildFacade.invite(guildId, PlayerGuildWebMapper.toInviteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/accept-invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long guildId) {
        guildFacade.acceptInvitation(guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/decline-invitation")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long guildId) {
        guildFacade.declineInvitation(guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/transfer-leader")
    public ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TransferLeader request
    ) {
        guildFacade.transferLeader(guildId, PlayerGuildWebMapper.toTransferLeaderCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Kick request
    ) {
        guildFacade.kick(guildId, PlayerGuildWebMapper.toKickCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.MemberOp request
    ) {
        guildFacade.promote(guildId, PlayerGuildWebMapper.toPromoteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/demote")
    public ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.MemberOp request
    ) {
        guildFacade.demote(guildId, PlayerGuildWebMapper.toDemoteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long guildId) {
        guildFacade.leave(guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/disband")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long guildId) {
        guildFacade.disband(guildId);
        return ApiResponses.ok(null);
    }
}
