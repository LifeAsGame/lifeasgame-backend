package online.lifeasgame.character.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerAchievementWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerHolderGrantRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerAchievementApiSpecV1;
import online.lifeasgame.character.application.AdminPlayerHolderGrantService;
import online.lifeasgame.character.application.PlayerAchievementService;
import online.lifeasgame.character.application.PlayerHolderQueryService;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerAchievementController implements AdminPlayerAchievementApiSpecV1 {

    private final AdminPlayerHolderGrantService holderGrantService;
    private final PlayerAchievementService playerAchievementService;
    private final PlayerHolderQueryService playerHolderQueryService;

    @Override
    @GetMapping("/{playerId}/achievements")
    public ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Infos>> getAchievements(
            @PathVariable Long playerId
    ) {
        List<PlayerAchievementResult.Info> result =
                playerHolderQueryService.getAchievementInfos(playerId);
        return ApiResponses.ok(
                AdminPlayerAchievementWebMapper.toInfos(playerId, result)
        );
    }

    @Override
    @PostMapping("/{playerId}/achievements/{achievementId}")
    public ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Granted>> grantAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody AdminPlayerHolderGrantRequest.Grant request
    ) {
        PlayerAchievementResult.Granted result = holderGrantService.grantAchievement(
                AdminPlayerAchievementWebMapper.toGrantCommand(
                        playerId,
                        achievementId,
                        request,
                        idempotencyKey,
                        correlationId
                )
        );
        return ApiResponses.ok(AdminPlayerAchievementWebMapper.toGranted(result));
    }

    @Override
    @DeleteMapping("/{playerId}/achievements/{achievementId}")
    public ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Revoked>> revokeAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    ) {
        PlayerAchievementResult.Revoked result = playerAchievementService.revokeAchievement(playerId, achievementId);
        return ApiResponses.deleted(AdminPlayerAchievementWebMapper.toRevoked(result));
    }
}
