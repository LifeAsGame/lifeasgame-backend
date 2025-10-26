package online.lifeasgame.social.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.request.PlayerGuildRequest;
import online.lifeasgame.social.api.player.response.PlayerGuildResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Tag(name = "Social Guild API V1 (Player)")
public interface PlayerGuildApiSpecV1 {

    // 생성/기본 변경
    @Operation(summary = "길드 생성")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> create(@Valid @RequestBody PlayerGuildRequest.Create request);

    @Operation(summary = "길드 이름 변경")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> rename(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Rename request
    );

    @Operation(summary = "길드 정책 변경")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changePolicy(
            @PathVariable Long guildId,
            @Valid @RequestBody
            PlayerGuildRequest.ChangePolicy request
    );

    @Operation(summary = "길드 설명 변경")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeDescription(
            @PathVariable Long guildId,
            @Valid @RequestBody
            PlayerGuildRequest.ChangeDescription request
    );

    @Operation(summary = "길드 엠블럼 변경")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> changeEmblem(
            @PathVariable Long guildId,
            @Valid @RequestBody
            PlayerGuildRequest.ChangeEmblem request
    );

    @Operation(summary = "태그 추가")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> addTag(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TagOp request
    );

    @Operation(summary = "태그 제거")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Info>> removeTag(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TagOp request
    );

    // 가입/권한
    @Operation(summary = "가입 요청")
    ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.RequestJoin request
    );

    @Operation(summary = "가입 승인(리더)")
    ResponseEntity<ApiResponse<Void>> approveJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Approve request
    );

    @Operation(summary = "가입 거절(리더)")
    ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Reject request
    );

    @Operation(summary = "가입 요청 취소(본인)")
    ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long guildId);

    @Operation(summary = "리더 위임")
    ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.TransferLeader request
    );

    @Operation(summary = "멤버 강퇴(오피서/리더)")
    ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Kick request
    );

    @Operation(summary = "오피서 승격(리더)")
    ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.MemberOp request
    );

    @Operation(summary = "멤버 강등(리더)")
    ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.MemberOp request
    );

    @Operation(summary = "길드 탈퇴")
    ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long guildId);

    @Operation(summary = "길드 해산(리더)")
    ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long guildId);

    // 초대
    @Operation(summary = "길드 초대(오피서/리더)")
    ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long guildId,
            @Valid @RequestBody PlayerGuildRequest.Invite request
    );

    @Operation(summary = "초대 수락")
    ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long guildId);

    @Operation(summary = "초대 거절")
    ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long guildId);

    // 조회
    @Operation(summary = "최근 길드 조회")
    ResponseEntity<ApiResponse<List<PlayerGuildResponse.Summary>>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    );

    @Operation(summary = "길드 검색")
    ResponseEntity<ApiResponse<PlayerGuildResponse.Page<PlayerGuildResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
