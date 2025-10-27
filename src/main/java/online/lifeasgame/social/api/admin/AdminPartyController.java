package online.lifeasgame.social.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.admin.mapper.AdminPartyWebMapper;
import online.lifeasgame.social.api.admin.request.AdminPartyRequest;
import online.lifeasgame.social.api.admin.response.AdminPartyResponse;
import online.lifeasgame.social.api.admin.spec.AdminPartyApiSpecV1;
import online.lifeasgame.social.application.PartyService;
import online.lifeasgame.social.application.result.PartyResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1")
public class AdminPartyController implements AdminPartyApiSpecV1 {

    private final PartyService partyService;

    @Override
    @GetMapping("/players/{playerId}/parties/{partyId}")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Info>> getPartyInfo(
            @PathVariable Long playerId,
            @PathVariable Long partyId
    ) {
        var info = partyService.getParty(playerId, partyId);
        return ApiResponses.ok(AdminPartyWebMapper.toInfo(info));
    }

    // ===== 일반 조회 =====
    @Override
    @GetMapping("/parties/search")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Page<AdminPartyResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PartyResult.Page<PartyResult.Summary> pages = partyService.search(
                keyword,
                visibility,
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );
        return ApiResponses.ok(AdminPartyWebMapper.toSummaryPage(pages));
    }

    @Override
    @GetMapping("/parties/recent")
    public ResponseEntity<ApiResponse<List<AdminPartyResponse.Summary>>> recent(
            @RequestParam(defaultValue="10") Integer limit
    ) {
        var infos = partyService.recent(Math.min(Math.max(limit, 1), 100));
        return ApiResponses.ok(AdminPartyWebMapper.toSummaries(infos));
    }

    // ===== 플레이어 스코프(acting as playerId) =====

    // 생성
    @Override
    @PostMapping("/players/{playerId}/parties")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Detail>> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPartyRequest.Create body
    ) {
        var info = partyService.create(playerId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(AdminPartyWebMapper.toDetail(info));
    }

    // 기본 변경
    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/rename")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Detail>> rename(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.Rename body
    ) {
        var info = partyService.rename(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(AdminPartyWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/policy")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Detail>> changePolicy(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.ChangePolicy body
    ) {
        var info = partyService.changePolicy(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(AdminPartyWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/description")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Detail>> changeDescription(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.ChangeDescription body
    ) {
        var info = partyService.changeDescription(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(AdminPartyWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/emblem")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Detail>> changeEmblem(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.ChangeEmblem body
    ) {
        var info = partyService.changeBanner(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(AdminPartyWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/tags/add")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Detail>> addTag(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.TagOp body
    ) {
        var info = partyService.addTag(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(AdminPartyWebMapper.toDetail(info));
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/tags/remove")
    public ResponseEntity<ApiResponse<AdminPartyResponse.Detail>> removeTag(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.TagOp body
    ) {
        var info = partyService.removeTag(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(AdminPartyWebMapper.toDetail(info));
    }

    // 가입/권한
    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/request-join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.RequestJoin body
    ) {
        partyService.requestJoin(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.Approve body
    ) {
        partyService.approveJoin(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.Reject body
    ) {
        partyService.rejectJoin(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/cancel-join")
    public ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long playerId, @PathVariable Long partyId) {
        partyService.cancelJoin(playerId, partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/transfer-leader")
    public ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.TransferLeader body
    ) {
        partyService.transferLeader(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.Kick body
    ) {
        partyService.kick(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.MemberOp body
    ) {
        partyService.promote(playerId, partyId, AdminPartyWebMapper.toCommandPromote(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/demote")
    public ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.MemberOp body
    ) {
        partyService.demote(playerId, partyId, AdminPartyWebMapper.toCommandDemote(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long playerId, @PathVariable Long partyId) {
        partyService.leave(playerId, partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/disband")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long playerId, @PathVariable Long partyId) {
        partyService.disbandByLeader(playerId, partyId);
        return ApiResponses.ok(null);
    }

    // 초대
    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long playerId,
            @PathVariable Long partyId,
            @Valid @RequestBody AdminPartyRequest.Invite body
    ) {
        partyService.invite(playerId, partyId, AdminPartyWebMapper.toCommand(body));
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/accept-invitation")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long playerId, @PathVariable Long partyId) {
        partyService.acceptInvitation(playerId, partyId);
        return ApiResponses.ok(null);
    }

    @Override
    @PostMapping("/players/{playerId}/parties/{partyId}/decline-invitation")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(
            @PathVariable Long playerId,
            @PathVariable Long partyId
    ) {
        partyService.declineInvitation(playerId, partyId);
        return ApiResponses.ok(null);
    }
}
