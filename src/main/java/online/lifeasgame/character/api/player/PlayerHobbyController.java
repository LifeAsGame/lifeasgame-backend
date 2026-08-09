package online.lifeasgame.character.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.PlayerHobbyWebMapper;
import online.lifeasgame.character.api.player.request.PlayerHobbyRequest;
import online.lifeasgame.character.api.player.response.PlayerHobbyResponse;
import online.lifeasgame.character.api.player.spec.PlayerHobbyApiSpecV1;
import online.lifeasgame.character.application.PlayerHobbyService;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerHobbyController implements PlayerHobbyApiSpecV1 {

    private final PlayerHobbyService playerHobbyService;

    @Override
    @GetMapping("/hobbies")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.Infos>> playerHobbyInfos() {
        List<PlayerHobbyResult.Info> results = playerHobbyService.getPlayerHobbyInfos();
        return ApiResponses.ok(PlayerHobbyWebMapper.toInfos(results));
    }

    @Override
    @PostMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.Created>> create(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.Create request
    ) {
        PlayerHobbyResult.Created result = playerHobbyService.createPlayerHobby(
                PlayerHobbyWebMapper.toCreateCommand(hobbyId, request)
        );

        return ApiResponses.created(
                URI.create("/api/v1/hobbies"),
                PlayerHobbyWebMapper.toCreated(result)
        );
    }

    @Override
    @PatchMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.Changed>> update(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.Update request
    ) {
        PlayerHobbyResult.Changed result = playerHobbyService.changePlayerHobby(
                PlayerHobbyWebMapper.toChangeCommand(hobbyId, request)
        );

        return ApiResponses.ok(PlayerHobbyWebMapper.toChanged(result));
    }

    @Override
    @DeleteMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<Long>> delete(@PathVariable Long hobbyId) {
        playerHobbyService.deletePlayerHobby(hobbyId);
        return ApiResponses.deleted(hobbyId);
    }
}
