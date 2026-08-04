package online.lifeasgame.character.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerTitleWebMapper;
import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerTitleApiSpecV1;
import online.lifeasgame.character.application.PlayerTitleService;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerTitleController implements AdminPlayerTitleApiSpecV1 {

    private final PlayerTitleService playerTitleService;

    @Override
    @PostMapping("/{playerId}/titles/{titleId}")
    public ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Granted>> grantTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    ) {
        PlayerTitleResult.Created result = playerTitleService.createTitle(playerId, titleId);
        return ApiResponses.ok(AdminPlayerTitleWebMapper.toGrantedTitle(result));
    }

    @Override
    @DeleteMapping("/{playerId}/titles/{titleId}")
    public ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Revoked>> revokeTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    ) {
        PlayerTitleResult.Revoked result = playerTitleService.revokeTitle(playerId, titleId);
        return ApiResponses.deleted(AdminPlayerTitleWebMapper.toRevoked(result));
    }
}
