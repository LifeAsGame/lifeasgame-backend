package online.lifeasgame.character.presentation;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerFacade;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.application.result.PlayerResult.PlayerInfo;
import online.lifeasgame.character.presentation.mapper.PlayerWebMapper;
import online.lifeasgame.character.presentation.request.PlayerRequest;
import online.lifeasgame.character.presentation.response.PlayerResponse;
import online.lifeasgame.character.presentation.spec.PlayerApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerController implements PlayerApiSpecV1 {

    private final PlayerFacade playerFacade;

    @Override
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PlayerResponse.Created>> linkStart(
            @Valid @RequestBody PlayerRequest.Register register
    ) {
        PlayerResult.Created playerResult = playerFacade.linkStart(PlayerWebMapper.toCommand(register));
        return ApiResponses.created(
                URI.create("/api/v1/players/" + playerResult.id()),
                PlayerWebMapper.toCreated(playerResult)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PlayerResponse.PlayerInfo>> playerInfo() {
        PlayerInfo playerInfo = playerFacade.getPlayerInfo();
        return ApiResponses.ok(
                PlayerWebMapper.toPlayerInfo(playerInfo)
        );
    }
}
