package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.social.application.GroupRosterService;
import online.lifeasgame.social.infra.GroupRosterStore.Entry;
import online.lifeasgame.social.infra.GroupRosterStore.Invitation;
import online.lifeasgame.social.infra.GroupRosterStore.Type;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupRosterController {
    private final GroupRosterService service;

    public record Fields(@NotBlank String displayName, String groupRoleLabel) {}
    public record Update(@NotBlank String displayName, String groupRoleLabel, @NotNull Long version) {}
    public record Version(@NotNull Long version) {}
    public record Invite(@NotNull @Positive Long targetPlayerId) {}
    public record Row(Long rosterEntryId, String groupType, Long groupId, String displayName,
                      String groupRoleLabel, long version, String status, Long linkedPlayerId,
                      String memberStatus) {
        static Row from(Entry e) {
            return new Row(e.id(), e.type().name(), e.groupId(), e.displayName(), e.groupRoleLabel(),
                    e.version(), e.linkedPlayerId() == null ? "UNLINKED" : "LINKED",
                    e.linkedPlayerId(), e.memberStatus());
        }
    }
    public record InvitationView(Long invitationId, String groupType, Long groupId, String groupName,
                                 Long rosterEntryId, String rosterDisplayName, Long targetPlayerId,
                                 Instant expiresAt, boolean membershipWillBeCreated, String status) {
        static InvitationView from(Invitation i) {
            return new InvitationView(i.id(), i.type().name(), i.groupId(), i.groupName(), i.entryId(),
                    i.rosterDisplayName(), i.targetPlayerId(), i.expiresAt(), i.membershipWillBeCreated(), i.status());
        }
    }
    public record InvitationPageView(List<InvitationView> contents, int page, int size,
                                     long totalElements, int totalPages) {
        static InvitationPageView from(GroupRosterService.InvitationPage p) {
            return new InvitationPageView(p.contents().stream().map(InvitationView::from).toList(),
                    p.page(), p.size(), p.totalElements(), p.totalPages());
        }
    }

    @GetMapping("/api/v1/{groupKind:guilds|parties}/{groupId}/roster")
    public ResponseEntity<ApiResponse<GroupRosterService.Page<Row>>> list(
            @PathVariable String groupKind, @PathVariable Long groupId,
            @RequestParam(defaultValue = "ALL") String status, @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = service.list(type(groupKind), groupId, status, keyword, page, size);
        return ApiResponses.ok(new GroupRosterService.Page<>(result.contents().stream().map(Row::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(), result.capabilities()));
    }

    @PostMapping("/api/v1/{groupKind:guilds|parties}/{groupId}/roster")
    public ResponseEntity<ApiResponse<Row>> create(@PathVariable String groupKind, @PathVariable Long groupId,
                                                    @Valid @RequestBody Fields request) {
        Row row = Row.from(service.create(type(groupKind), groupId, request.displayName(), request.groupRoleLabel()));
        return ApiResponses.created(URI.create("/api/v1/" + groupKind + "/" + groupId + "/roster/" + row.rosterEntryId()), row);
    }

    @PatchMapping("/api/v1/{groupKind:guilds|parties}/{groupId}/roster/{entryId}")
    public ResponseEntity<ApiResponse<Row>> update(@PathVariable String groupKind, @PathVariable Long groupId,
                                                    @PathVariable Long entryId, @Valid @RequestBody Update request) {
        return ApiResponses.ok(Row.from(service.update(type(groupKind), groupId, entryId,
                request.displayName(), request.groupRoleLabel(), request.version())));
    }

    @DeleteMapping("/api/v1/{groupKind:guilds|parties}/{groupId}/roster/{entryId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String groupKind, @PathVariable Long groupId,
                                                     @PathVariable Long entryId, @Valid @RequestBody Version request) {
        service.delete(type(groupKind), groupId, entryId, request.version());
        return ApiResponses.noContent();
    }

    @PostMapping("/api/v1/{groupKind:guilds|parties}/{groupId}/roster/{entryId}/invitations")
    public ResponseEntity<ApiResponse<InvitationView>> invite(@PathVariable String groupKind, @PathVariable Long groupId,
                                                               @PathVariable Long entryId, @Valid @RequestBody Invite request) {
        InvitationView invitation = InvitationView.from(service.invite(type(groupKind), groupId, entryId, request.targetPlayerId()));
        return ApiResponses.created(URI.create("/api/v1/roster-invitations/" + invitation.invitationId()), invitation);
    }

    @GetMapping("/api/v1/{groupKind:guilds|parties}/{groupId}/roster/invitations/pending")
    public ResponseEntity<ApiResponse<InvitationPageView>> pending(
            @PathVariable String groupKind, @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(InvitationPageView.from(service.pending(type(groupKind), groupId, page, size)));
    }

    @PostMapping("/api/v1/{groupKind:guilds|parties}/{groupId}/roster/invitations/{invitationId}/cancel")
    public ResponseEntity<ApiResponse<InvitationView>> cancel(@PathVariable String groupKind, @PathVariable Long groupId,
                                                                @PathVariable Long invitationId) {
        return ApiResponses.ok(InvitationView.from(service.cancel(type(groupKind), groupId, invitationId)));
    }

    @GetMapping("/api/v1/roster-invitations/mine")
    public ResponseEntity<ApiResponse<InvitationPageView>> mine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(InvitationPageView.from(service.mine(page, size)));
    }

    @PostMapping("/api/v1/roster-invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<Row>> accept(@PathVariable Long invitationId) {
        return ApiResponses.ok(Row.from(service.accept(invitationId)));
    }

    @PostMapping("/api/v1/roster-invitations/{invitationId}/decline")
    public ResponseEntity<ApiResponse<InvitationView>> decline(@PathVariable Long invitationId) {
        return ApiResponses.ok(InvitationView.from(service.decline(invitationId)));
    }

    private static Type type(String kind) { return "guilds".equals(kind) ? Type.GUILD : Type.PARTY; }
}
