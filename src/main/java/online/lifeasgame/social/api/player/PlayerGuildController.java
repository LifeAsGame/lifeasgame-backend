package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerGuildWebMapper;
import online.lifeasgame.social.api.player.request.PlayerGuildRequest;
import online.lifeasgame.social.api.player.response.PlayerGuildResponse;
import online.lifeasgame.social.api.player.spec.PlayerGuildApiSpecV1;
import online.lifeasgame.social.application.GuildService;
import online.lifeasgame.social.application.result.GuildResult;
import online.lifeasgame.social.domain.GuildWaitType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guilds")
public class PlayerGuildController implements PlayerGuildApiSpecV1 {

    private final GuildService guildService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Override
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PlayerGuildResponse.Summary>>> recent(
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        List<GuildResult.Summary> results = guildService.recent(Math.min(Math.max(limit, 1), 100));
        return ApiResponses.ok(PlayerGuildWebMapper.toSummaries(results));
    }

    @Override
    @GetMapping("/{guildId}")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> getGuildInfo(
            @PathVariable Long guildId
    ) {
        GuildResult.Info result = guildService.getGuild(player(), guildId);
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        GuildResult.Page<GuildResult.Summary> result = guildService.search(keyword, visibility, page, size);
        return ApiResponses.ok(PlayerGuildWebMapper.toSummaryPage(result));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.MyGuild>>> mine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerGuildWebMapper.toPage(guildService.mine(player(), page, size), PlayerGuildWebMapper::toMy));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.WaitMember>>> requests(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerGuildWebMapper.toPage(guildService.myPending(player(), GuildWaitType.JOIN_REQUEST, page, size), PlayerGuildWebMapper::toPending));
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.WaitMember>>> invitations(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerGuildWebMapper.toPage(guildService.myPending(player(), GuildWaitType.INVITATION, page, size), PlayerGuildWebMapper::toPending));
    }

    @GetMapping("/{guildId}/preview")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Summary>> preview(@PathVariable Long guildId) {
        return ApiResponses.ok(PlayerGuildWebMapper.toSummary(guildService.preview(guildId)));
    }

    @GetMapping("/{guildId}/members")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.Member>>> members(
            @PathVariable Long guildId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerGuildWebMapper.toPage(guildService.members(player(), guildId, page, size), PlayerGuildWebMapper::toMember));
    }

    @GetMapping("/{guildId}/pending-requests")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.WaitMember>>> pendingRequests(
            @PathVariable Long guildId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerGuildWebMapper.toPage(guildService.pendingRequests(player(), guildId, page, size), PlayerGuildWebMapper::toPending));
    }

    @GetMapping("/{guildId}/me")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Me>> me(@PathVariable Long guildId) {
        return ApiResponses.ok(PlayerGuildWebMapper.toMe(guildService.me(player(), guildId)));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> create(
            @Valid @RequestBody PlayerGuildRequest.Create request
    ) {
        GuildResult.Info result = guildService.create(player(), PlayerGuildWebMapper.toCreateCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/rename")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> rename(
            @PathVariable Long guildId,
            @Valid  @RequestBody PlayerGuildRequest.Rename request
    ) {
        GuildResult.Info result = guildService.rename(player(), guildId, PlayerGuildWebMapper.toRenameCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/policy")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changePolicy(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.ChangePolicy request
    ) {
        GuildResult.Info result = guildService.changePolicy(player(), guildId, PlayerGuildWebMapper.toChangePolicyCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/description")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeDescription(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.ChangeDescription request
    ) {
        GuildResult.Info result = guildService.changeDescription(player(), guildId, PlayerGuildWebMapper.toChangeDescriptionCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/emblem")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeEmblem(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.ChangeEmblem request
    ) {
        GuildResult.Info result = guildService.changeEmblem(player(), guildId, PlayerGuildWebMapper.toChangeEmblemCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/tags/add")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> addTag(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TagOp request
    ) {
        GuildResult.Info result = guildService.addTag(player(), guildId, PlayerGuildWebMapper.toTagOpCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/tags/remove")
    public ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> removeTag(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TagOp request
    ) {
        GuildResult.Info result = guildService.removeTag(player(), guildId, PlayerGuildWebMapper.toTagOpCommand(request));
        return ApiResponses.ok(PlayerGuildWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{guildId}/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.RequestJoin request
    ) {
        guildService.requestJoin(player(), guildId, PlayerGuildWebMapper.toRequestJoinCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long guildId) {
        guildService.cancelJoin(player(), guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Approve request
    ) {
        guildService.approveJoin(player(), guildId, PlayerGuildWebMapper.toApproveCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Reject request
    ) {
        guildService.rejectJoin(player(), guildId, PlayerGuildWebMapper.toRejectCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Invite request
    ) {
        guildService.invite(player(), guildId, PlayerGuildWebMapper.toInviteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/accept-invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long guildId) {
        guildService.acceptInvitation(player(), guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/decline-invitation")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long guildId) {
        guildService.declineInvitation(player(), guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/transfer-leader")
    public ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TransferLeader request
    ) {
        guildService.transferLeader(player(), guildId, PlayerGuildWebMapper.toTransferLeaderCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Kick request
    ) {
        guildService.kick(player(), guildId, PlayerGuildWebMapper.toKickCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.MemberOp request
    ) {
        guildService.promote(player(), guildId, PlayerGuildWebMapper.toPromoteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/demote")
    public ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.MemberOp request
    ) {
        guildService.demote(player(), guildId, PlayerGuildWebMapper.toDemoteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long guildId) {
        guildService.leave(player(), guildId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{guildId}/disband")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long guildId) {
        guildService.disbandByLeader(player(), guildId);
        return ApiResponses.ok(null);
    }
    private Long player() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }
}
