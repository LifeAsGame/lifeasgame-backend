package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerPartyWebMapper;
import online.lifeasgame.social.api.player.request.PlayerPartyRequest;
import online.lifeasgame.social.api.player.response.PlayerPartyResponse;
import online.lifeasgame.social.api.player.spec.PlayerPartyApiSpecV1;
import online.lifeasgame.social.application.PartyService;
import online.lifeasgame.social.application.result.PartyResult;
import online.lifeasgame.social.domain.PartyWaitType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parties")
public class PlayerPartyController implements PlayerPartyApiSpecV1 {

    private final PartyService partyService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Override
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PlayerPartyResponse.Summary>>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        var infos = partyService.recent(limit);
        return ApiResponses.ok(PlayerPartyWebMapper.toSummaries(infos));
    }

    @Override
    @GetMapping("/{partyId}")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> getPartyInfo(
            @PathVariable Long partyId
    ) {
        var info = partyService.getParty(player(), partyId);
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(info));
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Page<PlayerPartyResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PartyResult.Page<PartyResult.Summary> pages = partyService.search(keyword, visibility, page, size);
        return ApiResponses.ok(PlayerPartyWebMapper.toSummaryPage(pages));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Page<PlayerPartyResponse.MyParty>>> mine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerPartyWebMapper.toPage(partyService.mine(player(), page, size), PlayerPartyWebMapper::toMy));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Page<PlayerPartyResponse.WaitMember>>> requests(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerPartyWebMapper.toPage(partyService.myPending(player(), PartyWaitType.JOIN_REQUEST, page, size), PlayerPartyWebMapper::toPending));
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Page<PlayerPartyResponse.WaitMember>>> invitations(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerPartyWebMapper.toPage(partyService.myPending(player(), PartyWaitType.INVITATION, page, size), PlayerPartyWebMapper::toPending));
    }

    @GetMapping("/{partyId}/preview")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Summary>> preview(@PathVariable Long partyId) {
        return ApiResponses.ok(PlayerPartyWebMapper.toSummary(partyService.preview(partyId)));
    }

    @GetMapping("/{partyId}/members")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Page<PlayerPartyResponse.Member>>> members(
            @PathVariable Long partyId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerPartyWebMapper.toPage(partyService.members(player(), partyId, page, size), PlayerPartyWebMapper::toMember));
    }

    @GetMapping("/{partyId}/pending-requests")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Page<PlayerPartyResponse.WaitMember>>> pendingRequests(
            @PathVariable Long partyId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(PlayerPartyWebMapper.toPage(partyService.pendingRequests(player(), partyId, page, size), PlayerPartyWebMapper::toPending));
    }

    @GetMapping("/{partyId}/me")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Me>> me(@PathVariable Long partyId) {
        return ApiResponses.ok(PlayerPartyWebMapper.toMe(partyService.me(player(), partyId)));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> create(
            @Valid @RequestBody PlayerPartyRequest.Create request
    ) {
        PartyResult.Info result = partyService.create(player(), PlayerPartyWebMapper.toCreateCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/rename")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> rename(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.Rename request
    ) {
        PartyResult.Info result = partyService.rename(player(), partyId, PlayerPartyWebMapper.toRenameCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/policy")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changePolicy(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangePolicy request
    ) {
        PartyResult.Info result = partyService.changePolicy(player(), partyId, PlayerPartyWebMapper.toChangePolicyCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/description")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changeDescription(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangeDescription request
    ) {
        PartyResult.Info result = partyService.changeDescription(player(), partyId, PlayerPartyWebMapper.toChangeDescriptionCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/banner")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changeBanner(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangeBanner request
    ) {
        PartyResult.Info result = partyService.changeBanner(player(), partyId, PlayerPartyWebMapper.toChangeEmblemCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/tags/add")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> addTag(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.TagOp request
    ) {
        PartyResult.Info result = partyService.addTag(player(), partyId, PlayerPartyWebMapper.toTagOpCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/tags/remove")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> removeTag(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.TagOp request
    ) {
        PartyResult.Info result = partyService.removeTag(player(), partyId, PlayerPartyWebMapper.toTagOpCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.RequestJoin request
    ) {
        partyService.requestJoin(player(), partyId, PlayerPartyWebMapper.toRequestJoinCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long partyId) {
        partyService.cancelJoin(player(), partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Approve request
    ) {
        partyService.approveJoin(player(), partyId, PlayerPartyWebMapper.toApproveCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Reject request
    ) {
        partyService.rejectJoin(player(), partyId, PlayerPartyWebMapper.toRejectCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Invite request
    ) {
        partyService.invite(player(), partyId, PlayerPartyWebMapper.toInviteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/accept-invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long partyId) {
        partyService.acceptInvitation(player(), partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/decline-invitation")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long partyId) {
        partyService.declineInvitation(player(), partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/transfer-leader")
    public ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.TransferLeader request
    ) {
        partyService.transferLeader(player(), partyId, PlayerPartyWebMapper.toTransferLeaderCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Kick request
    ) {
        partyService.kick(player(), partyId, PlayerPartyWebMapper.toKickCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.MemberOp request
    ) {
        partyService.promote(player(), partyId, PlayerPartyWebMapper.toPromoteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/demote")
    public ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.MemberOp request
    ) {
        partyService.demote(player(), partyId, PlayerPartyWebMapper.toDemoteCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long partyId) {
        partyService.leave(player(), partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/disband")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long partyId) {
        partyService.disbandByLeader(player(), partyId);
        return ApiResponses.ok(null);
    }
    private Long player() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }
}
