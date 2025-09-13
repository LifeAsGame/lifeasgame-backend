package online.lifeasgame.character.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerTitleFacade;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.character.presentation.mapper.PlayerTitleWebMapper;
import online.lifeasgame.character.presentation.response.PlayerTitleResponse;
import online.lifeasgame.character.presentation.spec.PlayerTitleApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerTitleController implements PlayerTitleApiSpecV1 {

    private final PlayerTitleFacade playerTitleFacade;

    @Override
    @GetMapping("/titles")
    public ResponseEntity<ApiResponse<PlayerTitleResponse.PlayerTitleInfos>> playerTitleInfos() {
        List<PlayerTitleResult.PlayerTitleInfo> playerTitleInfos = playerTitleFacade.getPlayerTitleInfos();

        return ApiResponses.ok(
                PlayerTitleWebMapper.toPlayerTitleInfos(playerTitleInfos)
        );
    }
}
