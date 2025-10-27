package online.lifeasgame.social.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.request.PlayerPartyRequest;
import online.lifeasgame.social.api.player.response.PlayerPartyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Tag(name = "Social Party API V1 (Player)")
public interface PlayerPartyApiSpecV1 {

    @Operation(summary = "파티 생성")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> create(@Valid @RequestBody PlayerPartyRequest.Create request);

    @Operation(summary = "파티 이름 변경")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> rename(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Rename request
    );

    @Operation(summary = "파티 정책 변경")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changePolicy(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangePolicy request
    );

    @Operation(summary = "파티 설명 변경")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changeDescription(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangeDescription request
    );

    @Operation(summary = "파티 배너 변경")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> changeBanner(
            @PathVariable Long partyId,
            @Valid @RequestBody
            PlayerPartyRequest.ChangeBanner request
    );

    @Operation(summary = "태그 추가")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> addTag(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.TagOp request
    );

    @Operation(summary = "태그 제거")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Info>> removeTag(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.TagOp request
    );

    // 가입/권한
    @Operation(summary = "가입 요청")
    ResponseEntity<ApiResponse<Void>> requestJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.RequestJoin request
    );

    @Operation(summary = "가입 승인(리더)")
    ResponseEntity<ApiResponse<Void>> approveJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Approve request
    );

    @Operation(summary = "가입 거절(리더)")
    ResponseEntity<ApiResponse<Void>> rejectJoin(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Reject request
    );

    @Operation(summary = "가입 요청 취소(본인)")
    ResponseEntity<ApiResponse<Void>> cancelJoin(@PathVariable Long partyId);

    @Operation(summary = "리더 위임")
    ResponseEntity<ApiResponse<Void>> transferLeader(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.TransferLeader request
    );

    @Operation(summary = "멤버 강퇴(오피서/리더)")
    ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Kick request
    );

    @Operation(summary = "오피서 승격(리더)")
    ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.MemberOp request
    );

    @Operation(summary = "멤버 강등(리더)")
    ResponseEntity<ApiResponse<Void>> demote(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.MemberOp request
    );

    @Operation(summary = "파티 탈퇴(본인)")
    ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long partyId);

    @Operation(summary = "파티 해산(리더)")
    ResponseEntity<ApiResponse<Void>> disband(@PathVariable Long partyId);

    // 초대
    @Operation(summary = "파티 초대(오피서/리더)")
    ResponseEntity<ApiResponse<Void>> invite(
            @PathVariable Long partyId,
            @Valid @RequestBody PlayerPartyRequest.Invite request
    );

    @Operation(summary = "초대 수락")
    ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long partyId);

    @Operation(summary = "초대 거절")
    ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long partyId);

    // 조회
    @Operation(summary = "최근 파티 조회")
    ResponseEntity<ApiResponse<List<PlayerPartyResponse.Summary>>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    );

    @Operation(summary = "파티 검색")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Page<PlayerPartyResponse.Summary>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );


    @Operation(summary = "파티 상세 조회")
    ResponseEntity<ApiResponse<PlayerPartyResponse.Summary>> getPartyInfo(
            @PathVariable Long partyId
    );
}
