package online.lifeasgame.social.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.admin.request.AdminGuildRequest;
import online.lifeasgame.social.api.admin.response.AdminGuildResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Social Guild API V1 (Admin)")
public interface AdminGuildApiSpecV1 {

    @Operation(summary = "길드 단건 조회 (Admin)")
    ResponseEntity<ApiResponse<AdminGuildResponse.Info>> getGuildInfo(
            @PathVariable Long playerId,
            @PathVariable Long guildId
    );

    // 조회
    @Operation(summary = "길드 검색 (Admin)")
    ResponseEntity<ApiResponse<AdminGuildResponse.Page<AdminGuildResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "최근 길드 조회 (Admin)")
    ResponseEntity<ApiResponse<List<AdminGuildResponse.Summary>>> recent(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit
    );

    // 플레이어 스코프(acting as player)
    @Operation(summary = "플레이어로 길드 생성")
    ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminGuildRequest.Create request
    );

    @Operation(summary = "플레이어로 길드 이름 변경")
    ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> rename(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Rename request
    );

    @Operation(summary = "플레이어로 길드 정책 변경")
    ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> changePolicy(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody
            AdminGuildRequest.ChangePolicy request
    );

    @Operation(summary = "플레이어로 길드 설명 변경")
    ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> changeDescription(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody
            AdminGuildRequest.ChangeDescription request
    );

    @Operation(summary = "플레이어로 길드 엠블럼 변경")
    ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> changeEmblem(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody
            AdminGuildRequest.ChangeEmblem request
    );

    @Operation(summary = "플레이어로 태그 추가")
    ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> addTag(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.TagOp request
    );

    @Operation(summary = "플레이어로 태그 제거")
    ResponseEntity<ApiResponse<AdminGuildResponse.Detail>> removeTag(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.TagOp request
    );

    // 가입/권한
    @Operation(summary = "플레이어로 가입 요청")
    ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.RequestJoin request
    );

    @Operation(summary = "플레이어로 가입 승인")
    ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Approve request
    );

    @Operation(summary = "플레이어로 가입 거절")
    ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Reject request
    );

    @Operation(summary = "플레이어로 가입 요청 취소")
    ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long playerId, @PathVariable Long guildId);

    @Operation(summary = "플레이어로 리더 위임")
    ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.TransferLeader request
    );

    @Operation(summary = "플레이어로 멤버 강퇴")
    ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Kick request
    );

    @Operation(summary = "플레이어로 승격")
    ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.MemberOp request
    );

    @Operation(summary = "플레이어로 강등")
    ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.MemberOp request
    );

    @Operation(summary = "플레이어로 길드 탈퇴")
    ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long playerId, @PathVariable Long guildId);

    @Operation(summary = "플레이어로 길드 해산")
    ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long playerId, @PathVariable Long guildId);

    // 초대
    @Operation(summary = "플레이어로 초대")
    ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long playerId,
            @PathVariable Long guildId,
            @Valid @RequestBody AdminGuildRequest.Invite request
    );

    @Operation(summary = "플레이어로 초대 수락")
    ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long playerId, @PathVariable Long guildId);

    @Operation(summary = "플레이어로 초대 거절")
    ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long playerId, @PathVariable Long guildId);
}
