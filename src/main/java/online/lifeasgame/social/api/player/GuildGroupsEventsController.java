package online.lifeasgame.social.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.platform.web.validation.CalendarInstantDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import online.lifeasgame.social.application.GuildEventService;
import online.lifeasgame.social.application.GuildGroupLinkService;
import online.lifeasgame.social.application.GuildGroupCreator;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.api.player.request.PlayerPartyRequest;
import online.lifeasgame.social.application.result.GuildResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guilds/{guildId}")
public class GuildGroupsEventsController {
    private final GuildGroupLinkService links;
    private final GuildGroupCreator creator;
    private final GuildEventService events;

    public record Propose(@NotBlank String groupType, @NotNull @Positive Long groupId, @NotBlank String displayName) {}
    public record Label(@NotBlank String displayName) {}
    public record CreateGroup(@NotNull UUID clientRequestId, @NotBlank String groupType,
                              @NotBlank String displayName, @Valid PlayerPartyRequest.Create party,
                              @Valid RolePartyDetails roleParty) {
        public record RolePartyDetails(@NotNull @Positive Long roleId, @NotBlank String name,
                                       String description, int maxMembers) {}
        GuildGroupCreator.Command toCommand() {
            PartyCommand.Create p = party == null ? null : new PartyCommand.Create(party.name(), party.code(),
                    party.descriptionMd(), party.bannerImageUrl(), party.bannerBgColor(), party.visibility(),
                    party.joinPolicy(), party.maxMembers());
            GuildGroupCreator.RolePartyDetails rp = roleParty == null ? null :
                    new GuildGroupCreator.RolePartyDetails(roleParty.roleId(), roleParty.name(),
                            roleParty.description(), roleParty.maxMembers());
            return new GuildGroupCreator.Command(clientRequestId, groupType, displayName, p, rp);
        }
    }

    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<GuildGroupCreator.Result>> createGroup(
            @PathVariable Long guildId, @Valid @RequestBody CreateGroup request) {
        var result = creator.create(guildId, request.toCommand());
        return result.replayed() ? ApiResponses.ok(result) :
                ApiResponses.created(URI.create("/api/v1/guilds/" + guildId + "/group-links/" + result.linkId()), result);
    }
    public record EventDetails(@NotBlank String title, String sharedDescription,
                               @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant startsAt,
                               @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant endsAt,
                               String location) {
        GuildEventService.Details toCommand() {
            return new GuildEventService.Details(title, sharedDescription, startsAt, endsAt, location);
        }
    }

    @GetMapping("/group-links")
    public ResponseEntity<ApiResponse<GuildGroupLinkService.ActivePage>> links(
            @PathVariable Long guildId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(links.active(guildId, page, size));
    }

    @GetMapping("/group-links/pending")
    public ResponseEntity<ApiResponse<GuildResult.Page<GuildGroupLinkService.Link>>> pending(
            @PathVariable Long guildId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(links.pending(guildId, page, size));
    }

    @PostMapping("/group-links")
    public ResponseEntity<ApiResponse<GuildGroupLinkService.Link>> propose(
            @PathVariable Long guildId, @Valid @RequestBody Propose request) {
        GuildGroupLinkService.Link result = links.propose(guildId, request.groupType(), request.groupId(), request.displayName());
        return ApiResponses.created(URI.create("/api/v1/guilds/" + guildId + "/group-links/" + result.id()), result);
    }

    @PostMapping("/group-links/{linkId}/approve")
    public ResponseEntity<ApiResponse<GuildGroupLinkService.Link>> approve(
            @PathVariable Long guildId, @PathVariable Long linkId, @Valid @RequestBody Label request) {
        return ApiResponses.ok(links.approve(guildId, linkId, request.displayName()));
    }

    @PostMapping("/group-links/{linkId}/reject")
    public ResponseEntity<ApiResponse<GuildGroupLinkService.Link>> reject(@PathVariable Long guildId, @PathVariable Long linkId) {
        return ApiResponses.ok(links.reject(guildId, linkId));
    }

    @PostMapping("/group-links/{linkId}/cancel")
    public ResponseEntity<ApiResponse<GuildGroupLinkService.Link>> cancelLink(@PathVariable Long guildId, @PathVariable Long linkId) {
        return ApiResponses.ok(links.cancel(guildId, linkId));
    }

    @DeleteMapping("/group-links/{linkId}")
    public ResponseEntity<ApiResponse<Void>> unlink(@PathVariable Long guildId, @PathVariable Long linkId) {
        links.unlink(guildId, linkId);
        return ApiResponses.noContent();
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<GuildResult.Page<GuildEventService.Event>>> events(
            @PathVariable Long guildId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(events.list(guildId, page, size));
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<GuildEventService.Event>> createEvent(
            @PathVariable Long guildId, @Valid @RequestBody EventDetails request) {
        GuildEventService.Event result = events.create(guildId, request.toCommand());
        return ApiResponses.created(URI.create("/api/v1/guilds/" + guildId + "/events/" + result.id()), result);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<GuildEventService.Event>> event(@PathVariable Long guildId, @PathVariable Long eventId) {
        return ApiResponses.ok(events.detail(guildId, eventId));
    }

    @PatchMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<GuildEventService.Event>> updateEvent(
            @PathVariable Long guildId, @PathVariable Long eventId, @Valid @RequestBody EventDetails request) {
        return ApiResponses.ok(events.update(guildId, eventId, request.toCommand()));
    }

    @PostMapping("/events/{eventId}/complete")
    public ResponseEntity<ApiResponse<GuildEventService.Event>> complete(@PathVariable Long guildId, @PathVariable Long eventId) {
        return ApiResponses.ok(events.complete(guildId, eventId));
    }

    @PostMapping("/events/{eventId}/cancel")
    public ResponseEntity<ApiResponse<GuildEventService.Event>> cancelEvent(@PathVariable Long guildId, @PathVariable Long eventId) {
        return ApiResponses.ok(events.cancel(guildId, eventId));
    }

    @PutMapping("/events/{eventId}/rsvp")
    public ResponseEntity<ApiResponse<GuildEventService.Event>> rsvp(@PathVariable Long guildId, @PathVariable Long eventId) {
        return ApiResponses.ok(events.rsvp(guildId, eventId));
    }

    @DeleteMapping("/events/{eventId}/rsvp")
    public ResponseEntity<ApiResponse<Void>> withdraw(@PathVariable Long guildId, @PathVariable Long eventId) {
        events.withdraw(guildId, eventId);
        return ApiResponses.noContent();
    }

    @GetMapping("/events/{eventId}/participants")
    public ResponseEntity<ApiResponse<GuildResult.Page<GuildEventService.Participant>>> participants(
            @PathVariable Long guildId, @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(events.participants(guildId, eventId, page, size));
    }
}
