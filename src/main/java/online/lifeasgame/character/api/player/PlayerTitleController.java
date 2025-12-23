package online.lifeasgame.character.api.player;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.PlayerTitleWebMapper;
import online.lifeasgame.character.api.player.response.PlayerTitleResponse;
import online.lifeasgame.character.api.player.spec.PlayerTitleApiSpecV1;
import online.lifeasgame.character.application.PlayerTitleFacade;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerTitleController implements PlayerTitleApiSpecV1 {

    private final PlayerTitleFacade playerTitleFacade;

    @Override
    @GetMapping("/titles")
    public ResponseEntity<ApiResponse<PlayerTitleResponse.Infos>> playerTitleInfos() {
        List<PlayerTitleResult.Info> results = playerTitleFacade.getPlayerTitleInfos();
        return ApiResponses.ok(PlayerTitleWebMapper.toPlayerTitleInfos(results));
    }
}
