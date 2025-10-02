package online.lifeasgame.character.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerAchievementWebMapper;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerAchievementApiSpecV1;
import online.lifeasgame.character.application.PlayerAchievementService;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerAchievementController implements AdminPlayerAchievementApiSpecV1 {

    private final PlayerAchievementService adminPlayerAchievementService;

    @Override
    @PostMapping("/{playerId}/achievements/{achievementId}")
    public ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Granted>> grantAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    ) {
        PlayerAchievementResult.Granted granted = adminPlayerAchievementService.grantAchievement(playerId, achievementId);

        return ApiResponses.ok(
                AdminPlayerAchievementWebMapper.toGrantedAchievement(granted)
        );
    }
}
