package online.lifeasgame.character.api.player;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerAchievementFacade;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.api.player.mapper.PlayerAchievementWebMapper;
import online.lifeasgame.character.api.player.response.PlayerAchievementResponse;
import online.lifeasgame.character.api.player.spec.PlayerAchievementApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerAchievementController implements PlayerAchievementApiSpecV1 {

    private final PlayerAchievementFacade playerAchievementFacade;

    @Override
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<PlayerAchievementResponse.Infos>> playerAchievementInfos() {
        List<PlayerAchievementResult.Info> infos = playerAchievementFacade.getPlayerAchievementInfos();

        return ApiResponses.ok(
                PlayerAchievementWebMapper.toPlayerAchievementInfos(infos)
        );
    }
}
