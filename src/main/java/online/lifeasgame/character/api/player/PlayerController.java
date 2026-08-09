package online.lifeasgame.character.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.PlayerWebMapper;
import online.lifeasgame.character.api.player.request.PlayerRequest;
import online.lifeasgame.character.api.player.response.PlayerResponse;
import online.lifeasgame.character.api.player.spec.PlayerApiSpecV1;
import online.lifeasgame.character.application.PlayerFacade;
import online.lifeasgame.character.application.PlayerQueryService;
import online.lifeasgame.character.application.PlayerService;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerController implements PlayerApiSpecV1 {

    private final PlayerFacade playerFacade;
    private final PlayerQueryService playerQueryService;
    private final PlayerService playerService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PlayerResponse.Info>> me() {
        PlayerResult.PlayerInfo result = playerQueryService.getPlayerInfo();
        return ApiResponses.ok(PlayerWebMapper.toPlayerInfo(result));
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PlayerResponse.CreatedWithToken>> register(
            @Valid @RequestBody PlayerRequest.Register request
    ) {
        PlayerResult.CreatedWithToken result = playerFacade.linkStart(PlayerWebMapper.toRegisterCommand(request));
        return ApiResponses.created(
                URI.create("/api/v1/players/" + result.id()),
                PlayerWebMapper.toCreatedWithToken(result)
        );
    }

    @Override
    @PatchMapping("/titles/{titleId}")
    public ResponseEntity<ApiResponse<PlayerResponse.UpdatedTitle>> updateTitle(
            @PathVariable Long titleId
    ) {
        PlayerResult.UpdatedTitle result = playerService.changeRepresentativeTitle(titleId);
        return ApiResponses.ok(PlayerWebMapper.toUpdatedTitle(result));
    }
}
