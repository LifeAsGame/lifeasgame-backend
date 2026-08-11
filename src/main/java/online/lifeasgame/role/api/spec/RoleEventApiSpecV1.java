package online.lifeasgame.role.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.role.api.request.RoleEventRequest;
import online.lifeasgame.role.api.response.RoleEventResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Role Event API V1 (Player)")
public interface RoleEventApiSpecV1 {

    @Operation(summary = "Role Event 목록")
    ResponseEntity<ApiResponse<List<RoleEventResponse.Detail>>> list(
            @PathVariable Long roleId
    );

    @Operation(summary = "Role Event 상세")
    ResponseEntity<ApiResponse<RoleEventResponse.Detail>> detail(
            @PathVariable Long roleId,
            @PathVariable Long eventId
    );

    @Operation(summary = "Role Event 생성")
    ResponseEntity<ApiResponse<RoleEventResponse.Detail>> create(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleEventRequest.Create request
    );

    @Operation(summary = "Role Event 수정")
    ResponseEntity<ApiResponse<RoleEventResponse.Detail>> update(
            @PathVariable Long roleId,
            @PathVariable Long eventId,
            @Valid @RequestBody RoleEventRequest.Update request
    );

    @Operation(summary = "Role Event 완료")
    ResponseEntity<ApiResponse<RoleEventResponse.Detail>> complete(
            @PathVariable Long roleId,
            @PathVariable Long eventId
    );

    @Operation(summary = "Role Event 취소")
    ResponseEntity<ApiResponse<RoleEventResponse.Detail>> cancel(
            @PathVariable Long roleId,
            @PathVariable Long eventId
    );

    @Operation(summary = "Role Event 참여자 추가")
    ResponseEntity<ApiResponse<RoleEventResponse.Participant>> addParticipant(
            @PathVariable Long roleId,
            @PathVariable Long eventId,
            @Valid @RequestBody RoleEventRequest.AddParticipant request
    );

    @Operation(summary = "Role Event 참여자 제거")
    ResponseEntity<ApiResponse<Void>> removeParticipant(
            @PathVariable Long roleId,
            @PathVariable Long eventId,
            @PathVariable Long participantLinkId
    );
}
