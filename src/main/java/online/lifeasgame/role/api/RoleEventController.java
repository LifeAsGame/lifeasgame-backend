package online.lifeasgame.role.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.role.api.mapper.RoleEventWebMapper;
import online.lifeasgame.role.api.request.RoleEventRequest;
import online.lifeasgame.role.api.response.RoleEventResponse;
import online.lifeasgame.role.api.spec.RoleEventApiSpecV1;
import online.lifeasgame.role.application.RoleEventQueryService;
import online.lifeasgame.role.application.RoleEventService;
import online.lifeasgame.role.application.result.RoleEventResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles/{roleId}/events")
public class RoleEventController implements RoleEventApiSpecV1 {

    private final RoleEventService roleEventService;
    private final RoleEventQueryService roleEventQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleEventResponse.Detail>>> list(
            @PathVariable Long roleId
    ) {
        return ApiResponses.ok(roleEventQueryService.list(roleId).stream()
                .map(RoleEventWebMapper::toDetail)
                .toList());
    }

    @Override
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<RoleEventResponse.Detail>> detail(
            @PathVariable Long roleId,
            @PathVariable Long eventId
    ) {
        return ApiResponses.ok(RoleEventWebMapper.toDetail(
                roleEventQueryService.detail(roleId, eventId)
        ));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<RoleEventResponse.Detail>> create(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleEventRequest.Create request
    ) {
        RoleEventResult.Detail result = roleEventService.create(
                roleId,
                RoleEventWebMapper.toCreateCommand(request)
        );
        return ApiResponses.created(
                URI.create("/api/v1/roles/" + roleId + "/events/" + result.id()),
                RoleEventWebMapper.toDetail(result)
        );
    }

    @Override
    @PatchMapping("/{eventId}")
    public ResponseEntity<ApiResponse<RoleEventResponse.Detail>> update(
            @PathVariable Long roleId,
            @PathVariable Long eventId,
            @Valid @RequestBody RoleEventRequest.Update request
    ) {
        return ApiResponses.ok(RoleEventWebMapper.toDetail(
                roleEventService.update(
                        roleId,
                        eventId,
                        RoleEventWebMapper.toUpdateCommand(request)
                )
        ));
    }

    @Override
    @PostMapping("/{eventId}/complete")
    public ResponseEntity<ApiResponse<RoleEventResponse.Detail>> complete(
            @PathVariable Long roleId,
            @PathVariable Long eventId
    ) {
        return ApiResponses.ok(RoleEventWebMapper.toDetail(
                roleEventService.complete(roleId, eventId)
        ));
    }

    @Override
    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<ApiResponse<RoleEventResponse.Detail>> cancel(
            @PathVariable Long roleId,
            @PathVariable Long eventId
    ) {
        return ApiResponses.ok(RoleEventWebMapper.toDetail(
                roleEventService.cancel(roleId, eventId)
        ));
    }

    @Override
    @PostMapping("/{eventId}/participants")
    public ResponseEntity<ApiResponse<RoleEventResponse.Participant>> addParticipant(
            @PathVariable Long roleId,
            @PathVariable Long eventId,
            @Valid @RequestBody RoleEventRequest.AddParticipant request
    ) {
        return ApiResponses.ok(RoleEventWebMapper.toParticipant(
                roleEventService.addParticipant(
                        roleId,
                        eventId,
                        RoleEventWebMapper.toAddParticipantCommand(request)
                )
        ));
    }

    @Override
    @DeleteMapping("/{eventId}/participants/{participantLinkId}")
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @PathVariable Long roleId,
            @PathVariable Long eventId,
            @PathVariable Long participantLinkId
    ) {
        roleEventService.removeParticipant(
                roleId,
                eventId,
                participantLinkId
        );
        return ApiResponses.noContent();
    }
}
