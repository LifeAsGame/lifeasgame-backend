package online.lifeasgame.character.api.player;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerHobbyFacade;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.api.player.mapper.PlayerHobbyWebMapper;
import online.lifeasgame.character.api.player.request.PlayerHobbyRequest;
import online.lifeasgame.character.api.player.response.PlayerHobbyResponse;
import online.lifeasgame.character.api.player.spec.PlayerHobbyApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class PlayerHobbyController implements PlayerHobbyApiSpecV1 {

    private final PlayerHobbyFacade playerHobbyFacade;

    @Override
    @GetMapping("/hobbies")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.Infos>> playerHobbyInfos() {
        List<PlayerHobbyResult.Info> infos = playerHobbyFacade.getPlayerHobbyInfos();

        return ApiResponses.ok(
                PlayerHobbyWebMapper.toPlayerHobbyInfos(infos)
        );
    }

    @Override
    @PostMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.Created>> createPlayerHobby(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.Create request
    ) {
        PlayerHobbyResult.Created hobbyInfo = playerHobbyFacade.createPlayerHobby(
                PlayerHobbyWebMapper.toCommand(hobbyId, request)
        );
        return ApiResponses.created(
                URI.create("/api/v1/hobbies"),
                PlayerHobbyWebMapper.toCreatedPlayerHobby(hobbyInfo)
        );
    }

    @Override
    @PatchMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.Changed>> updatePlayerHobby(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.Change request
    ) {
        PlayerHobbyResult.Changed changed = playerHobbyFacade.changePlayerHobby(PlayerHobbyWebMapper.toCommand(hobbyId, request));
        return ApiResponses.ok(
                PlayerHobbyWebMapper.toChangedPlayerHobby(changed)
        );
    }

    @Override
    @DeleteMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<Long>> deletePlayerHobby(@PathVariable Long hobbyId) {
        playerHobbyFacade.deletePlayerHobby(hobbyId);
        return ApiResponses.deleted(hobbyId);
    }
}
