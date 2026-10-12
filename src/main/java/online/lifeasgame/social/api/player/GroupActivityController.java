package online.lifeasgame.social.api.player;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.platform.web.validation.CalendarInstantDeserializer;
import online.lifeasgame.social.application.GroupActivityResult;
import online.lifeasgame.social.application.GroupActivityService;
import online.lifeasgame.social.domain.ActivityGroupType;
import online.lifeasgame.social.domain.GroupActivityDetails;
import online.lifeasgame.social.domain.GroupActivityStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/{groupKind:parties|role-parties}/{groupId}/activities")
public class GroupActivityController {
    private final GroupActivityService service;

    public record Details(@NotBlank String title, String sharedDescription, String location,
                          @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant startsAt,
                          @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant endsAt) {
        GroupActivityDetails command() {
            return new GroupActivityDetails(title, sharedDescription, location, startsAt, endsAt);
        }
    }
    public record Create(@NotBlank String clientRequestId, @NotBlank String title,
                         String sharedDescription, String location,
                         @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant startsAt,
                         @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant endsAt) {
        GroupActivityDetails details() {
            return new GroupActivityDetails(title, sharedDescription, location, startsAt, endsAt);
        }
    }
    public record Edit(@NotNull @Min(0) Long version, @NotBlank String title,
                       String sharedDescription, String location,
                       @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant startsAt,
                       @NotNull @JsonDeserialize(using = CalendarInstantDeserializer.class) Instant endsAt) {
        GroupActivityDetails details() {
            return new GroupActivityDetails(title, sharedDescription, location, startsAt, endsAt);
        }
    }
    public record Version(@NotNull @Min(0) Long version) {}

    @GetMapping
    public ResponseEntity<ApiResponse<GroupActivityResult.ActivityPage>> list(
            @PathVariable String groupKind, @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(service.list(type(groupKind), groupId, page, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupActivityResult.Activity>> create(
            @PathVariable String groupKind, @PathVariable Long groupId, @Valid @RequestBody Create request) {
        var result = service.create(type(groupKind), groupId, request.clientRequestId(), request.details());
        return ApiResponses.created(URI.create("/api/v1/" + groupKind + "/" + groupId + "/activities/" + result.id()), result);
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ApiResponse<GroupActivityResult.Activity>> detail(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long activityId) {
        return ApiResponses.ok(service.detail(type(groupKind), groupId, activityId));
    }

    @PatchMapping("/{activityId}")
    public ResponseEntity<ApiResponse<GroupActivityResult.Activity>> update(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long activityId,
            @Valid @RequestBody Edit request) {
        return ApiResponses.ok(service.update(type(groupKind), groupId, activityId, request.version(), request.details()));
    }

    @PostMapping("/{activityId}/complete")
    public ResponseEntity<ApiResponse<GroupActivityResult.Activity>> complete(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long activityId,
            @Valid @RequestBody Version request) {
        return ApiResponses.ok(service.finish(type(groupKind), groupId, activityId, request.version(), GroupActivityStatus.COMPLETED));
    }

    @PostMapping("/{activityId}/cancel")
    public ResponseEntity<ApiResponse<GroupActivityResult.Activity>> cancel(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long activityId,
            @Valid @RequestBody Version request) {
        return ApiResponses.ok(service.finish(type(groupKind), groupId, activityId, request.version(), GroupActivityStatus.CANCELED));
    }

    @PutMapping("/{activityId}/rsvp")
    public ResponseEntity<ApiResponse<GroupActivityResult.Activity>> rsvp(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long activityId) {
        return ApiResponses.ok(service.rsvp(type(groupKind), groupId, activityId));
    }

    @DeleteMapping("/{activityId}/rsvp")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long activityId) {
        service.withdraw(type(groupKind), groupId, activityId);
        return ApiResponses.noContent();
    }

    @GetMapping("/{activityId}/participants")
    public ResponseEntity<ApiResponse<GroupActivityResult.Page<GroupActivityResult.Participant>>> participants(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long activityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(service.participants(type(groupKind), groupId, activityId, page, size));
    }

    @GetMapping("/editors")
    public ResponseEntity<ApiResponse<GroupActivityResult.Page<GroupActivityResult.Editor>>> editors(
            @PathVariable String groupKind, @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponses.ok(service.editors(type(groupKind), groupId, page, size));
    }

    @PutMapping("/editors/{playerId}")
    public ResponseEntity<ApiResponse<Void>> grant(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long playerId) {
        service.grant(type(groupKind), groupId, playerId);
        return ApiResponses.noContent();
    }

    @DeleteMapping("/editors/{playerId}")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @PathVariable String groupKind, @PathVariable Long groupId, @PathVariable Long playerId) {
        service.revoke(type(groupKind), groupId, playerId);
        return ApiResponses.noContent();
    }

    private static ActivityGroupType type(String path) {
        return path.equals("parties") ? ActivityGroupType.PARTY : ActivityGroupType.ROLE_PARTY;
    }
}
