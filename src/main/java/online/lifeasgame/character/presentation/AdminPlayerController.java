package online.lifeasgame.character.presentation;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminPlayerService;
import online.lifeasgame.character.application.result.AdminPlayerResult.ExpGranted;
import online.lifeasgame.character.presentation.mapper.AdminPlayerWebMapper;
import online.lifeasgame.character.presentation.request.AdminPlayerRequest;
import online.lifeasgame.character.presentation.response.AdminPlayerResponse;
import online.lifeasgame.character.presentation.spec.AdminPlayerApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerController implements AdminPlayerApiSpecV1 {

    private final AdminPlayerService adminPlayerService;

    @Override
    @PostMapping("/exp/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.ExpGranted>> grantExp(AdminPlayerRequest.GrantExp request) {
        ExpGranted expGranted = adminPlayerService.grantExp(request.playerId(), request.exp());
        return ApiResponses.ok(
                AdminPlayerWebMapper.toExpGranted(expGranted)
        );
    }
}
