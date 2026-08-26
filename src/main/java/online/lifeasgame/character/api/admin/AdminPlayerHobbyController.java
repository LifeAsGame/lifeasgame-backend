package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerHobbyWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerHobbyApiSpecV1;
import online.lifeasgame.character.application.PlayerHobbyService;
import online.lifeasgame.character.application.PlayerHolderQueryService;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerHobbyController implements AdminPlayerHobbyApiSpecV1 {

    private final PlayerHobbyService playerHobbyService;
    private final PlayerHolderQueryService playerHolderQueryService;

    @Override
    @GetMapping("/{playerId}/hobbies")
    public ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.Infos>> getHobbies(
            @PathVariable Long playerId
    ) {
        List<PlayerHobbyResult.Info> result =
                playerHolderQueryService.getHobbyInfos(playerId);
        return ApiResponses.ok(
                AdminPlayerHobbyWebMapper.toInfos(playerId, result)
        );
    }

    @Override
    @PostMapping("/{playerId}/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.Granted>> grantHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId,
            @Valid @RequestBody AdminPlayerHobbyRequest.Grant request
    ) {
        PlayerHobbyResult.Created result = playerHobbyService.createPlayerHobby(
                playerId,
                AdminPlayerHobbyWebMapper.toCreatedCommand(hobbyId, request)
        );

        return ApiResponses.ok(AdminPlayerHobbyWebMapper.toGranted(result));
    }

    @Override
    @DeleteMapping("/{playerId}/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.Revoked>> revokeHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId
    ) {
        PlayerHobbyResult.Revoked result = playerHobbyService.revokeHobby(playerId, hobbyId);
        return ApiResponses.ok(AdminPlayerHobbyWebMapper.toRevoked(result));
    }
}
