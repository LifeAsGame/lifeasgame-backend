package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerHobbyWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerHobbyApiSpecV1;
import online.lifeasgame.character.application.PlayerHobbyService;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerHobbyController implements AdminPlayerHobbyApiSpecV1 {

    private final PlayerHobbyService adminPlayerHobbyService;

    @Override
    @PostMapping("/{playerId}/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.GrantedHobby>> grantHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId,
            @Valid @RequestBody AdminPlayerHobbyRequest.GrantHobby request
    ) {
        PlayerHobbyResult.GrantedHobby grantedHobby = adminPlayerHobbyService.grantHobby(
                AdminPlayerHobbyWebMapper.toCommand(playerId, hobbyId, request)
        );

        return ApiResponses.ok(
                AdminPlayerHobbyWebMapper.toGrantedHobby(grantedHobby)
        );
    }
}
