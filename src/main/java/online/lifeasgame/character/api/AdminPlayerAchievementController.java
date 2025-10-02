package online.lifeasgame.character.api;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminPlayerAchievementService;
import online.lifeasgame.character.application.result.AdminPlayerAchievementResult;
import online.lifeasgame.character.api.mapper.AdminPlayerAchievementWebMapper;
import online.lifeasgame.character.api.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.api.spec.AdminPlayerAchievementApiSpecV1;
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

    private final AdminPlayerAchievementService adminPlayerAchievementService;

    @Override
    @PostMapping("/{playerId}/achievements/{achievementId}")
    public ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.GrantedAchievement>> grantAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    ) {
        AdminPlayerAchievementResult.GrantedAchievement grantedAchievement = adminPlayerAchievementService.grantAchievement(playerId, achievementId);

        return ApiResponses.ok(
                AdminPlayerAchievementWebMapper.toGrantedAchievement(grantedAchievement)
        );
    }
}
