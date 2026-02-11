package online.lifeasgame.social.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.response.PlayerPartyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface PlayerPartyApiDraftSpecV1 {

    @Operation(summary = "내 파티 목록(텍스트 UI용)")
    ResponseEntity<ApiResponse<List<PlayerPartyResponse.MyParty>>> myParties();

    @Operation(summary = "파티 멤버 목록(텍스트 UI용)")
    ResponseEntity<ApiResponse<List<PlayerPartyResponse.Member>>> members(@PathVariable Long partyId);

    @Operation(summary = "파티 가입 신청 목록(리더/오피서)")
    ResponseEntity<ApiResponse<List<PlayerPartyResponse.WaitMember>>> joinRequests(@PathVariable Long partyId);

    @Operation(summary = "파티 초대 목록(리더/오피서)")
    ResponseEntity<ApiResponse<List<PlayerPartyResponse.WaitMember>>> invitations(@PathVariable Long partyId);

    @Operation(summary = "내가 받은 파티 초대 목록")
    ResponseEntity<ApiResponse<List<PlayerPartyResponse.WaitMember>>> myInvitations();

    @Operation(summary = "내가 보낸 가입 신청 목록")
    ResponseEntity<ApiResponse<List<PlayerPartyResponse.WaitMember>>> myJoinRequests();

}
