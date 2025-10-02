package online.lifeasgame.character.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerHobbyFacade;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.api.mapper.PlayerHobbyWebMapper;
import online.lifeasgame.character.api.request.PlayerHobbyRequest;
import online.lifeasgame.character.api.response.PlayerHobbyResponse;
import online.lifeasgame.character.api.spec.PlayerHobbyApiSpecV1;
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
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.PlayerHobbyInfos>> playerHobbyInfos() {
        List<PlayerHobbyResult.PlayerHobbyInfo> playerHobbyInfos = playerHobbyFacade.getPlayerHobbyInfos();

        return ApiResponses.ok(
                PlayerHobbyWebMapper.toPlayerHobbyInfos(playerHobbyInfos)
        );
    }

    @Override
    @PostMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.CreatedPlayerHobby>> createPlayerHobby(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.CreatePlayerHobby request
    ) {
        PlayerHobbyResult.CreatedPlayerHobby hobbyInfo = playerHobbyFacade.createPlayerHobby(
                PlayerHobbyWebMapper.toCommand(hobbyId, request)
        );
        return ApiResponses.created(
                URI.create("/api/v1/hobbies"),
                PlayerHobbyWebMapper.toCreatedPlayerHobby(hobbyInfo)
        );
    }

    @Override
    @PatchMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<PlayerHobbyResponse.ChangedPlayerHobby>> updatePlayerHobby(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.ChangePlayerHobby request
    ) {
        PlayerHobbyResult.ChangedPlayerHobby changedPlayerHobby = playerHobbyFacade.changePlayerHobby(PlayerHobbyWebMapper.toCommand(hobbyId, request));
        return ApiResponses.ok(
                PlayerHobbyWebMapper.toChangedPlayerHobby(changedPlayerHobby)
        );
    }

    @Override
    @DeleteMapping("/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<Long>> deletePlayerHobby(@PathVariable Long hobbyId) {
        playerHobbyFacade.deletePlayerHobby(hobbyId);
        return ApiResponses.deleted(hobbyId);
    }
}
