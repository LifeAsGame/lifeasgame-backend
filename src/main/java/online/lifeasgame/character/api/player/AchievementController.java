package online.lifeasgame.character.api.player;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AchievementService;
import online.lifeasgame.character.application.result.AchievementResult.Info;
import online.lifeasgame.character.api.player.mapper.AchievementWebMapper;
import online.lifeasgame.character.api.player.response.AchievementResponse;
import online.lifeasgame.character.api.player.spec.AchievementApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/achievements")
public class AchievementController implements AchievementApiSpecV1 {

    private final AchievementService achievementService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AchievementResponse.AchievementInfos>> achievementInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    ) {
        List<Info> infos = achievementService.getAchievements(categories);
        return ApiResponses.ok(
                AchievementWebMapper.toAchievementInfos(infos)
        );
    }
}
