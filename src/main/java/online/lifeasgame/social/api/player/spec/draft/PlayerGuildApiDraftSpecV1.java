package online.lifeasgame.social.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.social.api.player.response.PlayerGuildResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface PlayerGuildApiDraftSpecV1 {

    @Operation(summary = "내 길드 목록(텍스트 UI용)")
    ResponseEntity<ApiResponse<List<PlayerGuildResponse.MyGuild>>> myGuilds();

    @Operation(summary = "길드 멤버 목록(텍스트 UI용)")
    ResponseEntity<ApiResponse<List<PlayerGuildResponse.Member>>> members(@PathVariable Long guildId);

    @Operation(summary = "길드 가입 신청 목록(리더/오피서)")
    ResponseEntity<ApiResponse<List<PlayerGuildResponse.WaitMember>>> joinRequests(@PathVariable Long guildId);

    @Operation(summary = "길드 초대 목록(리더/오피서)")
    ResponseEntity<ApiResponse<List<PlayerGuildResponse.WaitMember>>> invitations(@PathVariable Long guildId);

    @Operation(summary = "내가 받은 길드 초대 목록")
    ResponseEntity<ApiResponse<List<PlayerGuildResponse.WaitMember>>> myInvitations();

    @Operation(summary = "내가 보낸 가입 신청 목록")
    ResponseEntity<ApiResponse<List<PlayerGuildResponse.WaitMember>>> myJoinRequests();

}
