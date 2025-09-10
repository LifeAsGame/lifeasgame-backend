package online.lifeasgame.character.presentation;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerFacade;
import online.lifeasgame.character.application.PlayerService;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.presentation.mapper.PlayerWebMapper;
import online.lifeasgame.character.presentation.request.PlayerRequest;
import online.lifeasgame.character.presentation.response.PlayerResponse;
import online.lifeasgame.character.presentation.spec.PlayerApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerController implements PlayerApiSpecV1 {

    private final PlayerFacade playerFacade;
    private final PlayerService playerService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PlayerResponse.Created>> linkStart(
            @Valid @RequestBody PlayerRequest.Register request
    ) {
        PlayerResult.Created playerResult = playerFacade.linkStart(PlayerWebMapper.toCommand(request));
        return ApiResponses.created(
                URI.create("/api/v1/players/" + playerResult.id()),
                PlayerWebMapper.toCreated(playerResult)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PlayerResponse.PlayerInfo>> playerInfo() {
        PlayerResult.PlayerInfo playerInfo = playerFacade.getPlayerInfo();
        return ApiResponses.ok(
                PlayerWebMapper.toPlayerInfo(playerInfo)
        );
    }

    @Override
    @PatchMapping("/{playerId}/health/current")
    public ResponseEntity<ApiResponse<PlayerResponse.CurrentHp>> updateCurrentHp(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeHp request
    ){
        PlayerResult.CurrentHp currentHp = playerService.changeHp(PlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                PlayerWebMapper.toCurrentHp(currentHp)
        );
    }

    @Override
    @PatchMapping("/{playerId}/health/capacity")
    public ResponseEntity<ApiResponse<PlayerResponse.HpCapacity>> updateHpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeHpCapacity request
    ) {
        PlayerResult.HpCapacity hpCapacity = playerService.changeHpCapacity(PlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                PlayerWebMapper.toHpCapacity(hpCapacity)
        );
    }

    @Override
    @PatchMapping("/{playerId}/mana/current")
    public ResponseEntity<ApiResponse<PlayerResponse.CurrentMp>> updateCurrentMp(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeMp request
    ){
        PlayerResult.CurrentMp currentMp = playerService.changeMp(PlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                PlayerWebMapper.toCurrentMp(currentMp)
        );
    }

    @Override
    @PatchMapping("/{playerId}/mana/capacity")
    public ResponseEntity<ApiResponse<PlayerResponse.MpCapacity>> updateMpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeMpCapacity request
    ) {
        PlayerResult.MpCapacity mpCapacity = playerService.changeMpCapacity(PlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                PlayerWebMapper.toMpCapacity(mpCapacity)
        );
    }
}
