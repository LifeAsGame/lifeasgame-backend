package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.mapper.PlayerPartyWebMapper;
import online.lifeasgame.social.api.player.request.PlayerPartyRequest;
import online.lifeasgame.social.api.player.response.PlayerPartyResponse;
import online.lifeasgame.social.api.player.spec.PlayerPartyApiSpecV1;
import online.lifeasgame.social.application.PartyFacade;
import online.lifeasgame.social.application.result.PartyResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parties")
public class PlayerPartyController implements PlayerPartyApiSpecV1 {

    private final PartyFacade partyFacade;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> create(
            @Valid @RequestBody PlayerPartyRequest.Create request
    ) {
        PartyResult.Info result = partyFacade.create(PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/rename")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> rename(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.Rename request
    ) {
        PartyResult.Info result = partyFacade.rename(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/policy")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changePolicy(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangePolicy request
    ) {
        PartyResult.Info result = partyFacade.changePolicy(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/description")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changeDescription(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangeDescription request
    ) {
        PartyResult.Info result = partyFacade.changeDescription(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/banner")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changeBanner(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangeBanner request
    ) {
        PartyResult.Info result = partyFacade.changeBanner(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/tags/add")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> addTag(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.TagOp request
    ) {
        PartyResult.Info result = partyFacade.addTag(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    @Override
    @PostMapping("/{partyId}/tags/remove")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> removeTag(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.TagOp request
    ) {
        PartyResult.Info result = partyFacade.removeTag(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(PlayerPartyWebMapper.toInfo(result));
    }

    // 가입/권한
    @Override
    @PostMapping("/{partyId}/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.RequestJoin request
    ) {
        partyFacade.requestJoin(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Approve request
    ) {
        partyFacade.approveJoin(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Reject request
    ) {
        partyFacade.rejectJoin(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long partyId) {
        partyFacade.cancelJoin(partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/transfer-leader")
    public ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.TransferLeader request
    ) {
        partyFacade.transferLeader(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Kick request
    ) {
        partyFacade.kick(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.MemberOp request
    ) {
        partyFacade.promote(partyId, PlayerPartyWebMapper.toCommandPromote(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/demote")
    public ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.MemberOp request
    ) {
        partyFacade.demote(partyId, PlayerPartyWebMapper.toCommandDemote(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long partyId) {
        partyFacade.leave(partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/disband")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long partyId) {
        partyFacade.disband(partyId);
        return ApiResponses.ok(null);
    }

    // 초대
    @Override
    @PostMapping("/{partyId}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Invite request
    ) {
        partyFacade.invite(partyId, PlayerPartyWebMapper.toCommand(request));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/accept-invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long partyId) {
        partyFacade.acceptInvitation(partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/{partyId}/decline-invitation")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long partyId) {
        partyFacade.declineInvitation(partyId);
        return ApiResponses.ok(null);
    }

    // 조회
    @Override
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PlayerPartyResponse.Summary>>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    ) {
        var infos = partyFacade.recent(limit);
        return ApiResponses.ok(PlayerPartyWebMapper.toSummaryList(infos));
    }

    @Override
    @GetMapping("/{partyId}")
    public ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> getPartyInfo(
            @PathVariable Long partyId
    ) {
        var info = partyFacade.getParty(partyId);
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
        PartyResult.Page<PartyResult.Summary> pages = partyFacade.search(keyword, visibility, page, size);
        return ApiResponses.ok(PlayerPartyWebMapper.toSummaryPage(pages));
    }
}
