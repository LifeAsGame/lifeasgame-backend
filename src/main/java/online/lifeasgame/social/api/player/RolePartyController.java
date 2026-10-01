package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.api.player.request.RolePartyRequest;
import online.lifeasgame.social.application.RolePartyResult;
import online.lifeasgame.social.application.RolePartyService;
import online.lifeasgame.social.application.RolePartyQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RolePartyController {
    private final RolePartyService service;
    private final RolePartyQueryService queryService;

    @GetMapping("/roles/{roleId}/role-parties")
    public ResponseEntity<ApiResponse<RolePartyResult.PageResult<RolePartyResult.Summary>>> forRole(
            @PathVariable Long roleId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(queryService.forRole(roleId, page, size));
    }

    @PostMapping("/roles/{roleId}/role-parties")
    public ResponseEntity<ApiResponse<RolePartyResult.Detail>> create(
            @PathVariable Long roleId, @Valid @RequestBody RolePartyRequest.Details request) {
        RolePartyResult.Detail result = service.create(roleId, request.name(), request.description(), request.maxMembers());
        return ApiResponses.created(URI.create("/api/v1/role-parties/" + result.id()), result);
    }

    @GetMapping("/role-parties/mine")
    public ResponseEntity<ApiResponse<RolePartyResult.PageResult<RolePartyResult.MyGroup>>> mine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(queryService.mine(page, size));
    }

    @GetMapping("/role-parties/invitations/mine")
    public ResponseEntity<ApiResponse<RolePartyResult.PageResult<RolePartyResult.Invitation>>> myInvitations(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(queryService.myInvitations(page, size));
    }

    @GetMapping("/role-parties/{id}")
    public ResponseEntity<ApiResponse<RolePartyResult.Detail>> detail(@PathVariable Long id) {
        return ApiResponses.ok(queryService.detail(id));
    }

    @GetMapping("/role-parties/{id}/members")
    public ResponseEntity<ApiResponse<RolePartyResult.PageResult<RolePartyResult.Member>>> members(
            @PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(queryService.members(id, page, size));
    }

    @PatchMapping("/role-parties/{id}")
    public ResponseEntity<ApiResponse<RolePartyResult.Detail>> update(
            @PathVariable Long id, @Valid @RequestBody RolePartyRequest.Details request) {
        return ApiResponses.ok(service.update(id, request.name(), request.description(), request.maxMembers()));
    }

    @PostMapping("/role-parties/{id}/invitations")
    public ResponseEntity<ApiResponse<RolePartyResult.Invitation>> invite(
            @PathVariable Long id, @Valid @RequestBody RolePartyRequest.Invite request) {
        return ApiResponses.ok(service.invite(id, request.inviteePlayerId()));
    }

    @PostMapping("/role-parties/{id}/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<RolePartyResult.Detail>> accept(@PathVariable Long id, @PathVariable Long invitationId) {
        return ApiResponses.ok(service.accept(id, invitationId));
    }

    @PostMapping("/role-parties/{id}/invitations/{invitationId}/decline")
    public ResponseEntity<ApiResponse<Void>> decline(@PathVariable Long id, @PathVariable Long invitationId) {
        service.decline(id, invitationId);
        return ApiResponses.noContent();
    }

    @DeleteMapping("/role-parties/{id}/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(@PathVariable Long id, @PathVariable Long invitationId) {
        service.cancelInvitation(id, invitationId);
        return ApiResponses.noContent();
    }

    @PostMapping("/role-parties/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long id) {
        service.leave(id);
        return ApiResponses.noContent();
    }

    @PostMapping("/role-parties/{id}/transfer-leader")
    public ResponseEntity<ApiResponse<RolePartyResult.Detail>> transferLeader(
            @PathVariable Long id, @Valid @RequestBody RolePartyRequest.Transfer request) {
        return ApiResponses.ok(service.transferLeader(id, request.toPlayerId()));
    }

    @PostMapping("/role-parties/{id}/disband")
    public ResponseEntity<ApiResponse<RolePartyResult.Detail>> disband(@PathVariable Long id) {
        return ApiResponses.ok(service.disband(id));
    }
}
