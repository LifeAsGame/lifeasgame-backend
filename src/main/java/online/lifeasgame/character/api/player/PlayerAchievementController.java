package online.lifeasgame.character.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.PlayerAchievementWebMapper;
import online.lifeasgame.character.api.player.response.PlayerAchievementResponse;
import online.lifeasgame.character.api.player.spec.PlayerAchievementApiSpecV1;
import online.lifeasgame.character.application.PlayerAchievementService;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerAchievementController implements PlayerAchievementApiSpecV1 {

    private final PlayerAchievementService playerAchievementService;

    @Override
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<PlayerAchievementResponse.Infos>> playerAchievementInfos() {
        List<PlayerAchievementResult.Info> results = playerAchievementService.getPlayerAchievementInfos();
        return ApiResponses.ok(PlayerAchievementWebMapper.toInfos(results));
    }

    @Override
    @GetMapping("/achievements/{achievementId}")
    public ResponseEntity<ApiResponse<PlayerAchievementResponse.Info>> playerAchievementInfo(
            @PathVariable Long achievementId
    ) {
        PlayerAchievementResult.Info result = playerAchievementService.getPlayerAchievementInfo(achievementId);
        return ApiResponses.ok(PlayerAchievementWebMapper.toInfo(result));
    }
}
