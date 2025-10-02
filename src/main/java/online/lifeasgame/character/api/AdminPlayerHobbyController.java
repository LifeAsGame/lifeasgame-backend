package online.lifeasgame.character.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminPlayerHobbyService;
import online.lifeasgame.character.application.result.AdminPlayerHobbyResult;
import online.lifeasgame.character.api.mapper.AdminPlayerHobbyWebMapper;
import online.lifeasgame.character.api.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.response.AdminPlayerHobbyResponse;
import online.lifeasgame.character.api.spec.AdminPlayerHobbyApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerHobbyController implements AdminPlayerHobbyApiSpecV1 {

    private final AdminPlayerHobbyService adminPlayerHobbyService;

    @Override
    @PostMapping("/{playerId}/hobbies/{hobbyId}")
    public ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.GrantedHobby>> grantHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId,
            @Valid @RequestBody AdminPlayerHobbyRequest.GrantHobby request
    ) {
        AdminPlayerHobbyResult.GrantedHobby grantedHobby = adminPlayerHobbyService.grantHobby(
                AdminPlayerHobbyWebMapper.toCommand(playerId, hobbyId, request)
        );

        return ApiResponses.ok(
                AdminPlayerHobbyWebMapper.toGrantedHobby(grantedHobby)
        );
    }
}
