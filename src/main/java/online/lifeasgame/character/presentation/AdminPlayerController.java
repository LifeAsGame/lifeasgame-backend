package online.lifeasgame.character.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminPlayerService;
import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.application.result.AdminPlayerResult.ExpGranted;
import online.lifeasgame.character.presentation.mapper.AdminPlayerWebMapper;
import online.lifeasgame.character.presentation.request.AdminPlayerRequest;
import online.lifeasgame.character.presentation.response.AdminPlayerResponse;
import online.lifeasgame.character.presentation.spec.AdminPlayerApiSpecV1;
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
public class AdminPlayerController implements AdminPlayerApiSpecV1 {

    private final AdminPlayerService adminPlayerService;

    @Override
    @PostMapping("/{playerId}/exp/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.ExpGranted>> grantExp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantExp request
    ) {
        ExpGranted expGranted = adminPlayerService.grantExp(playerId, request.exp());
        return ApiResponses.ok(
                AdminPlayerWebMapper.toExpGranted(expGranted)
        );
    }

    @Override
    @PostMapping("/{playerId}/core-stats/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.CoreStatsGranted>> grantCoreStats(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantCoreStats request
    ) {
        AdminPlayerResult.CoreStatsGranted coreStatsGranted =
                adminPlayerService.grantCoreStats(AdminPlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                AdminPlayerWebMapper.toCoreStatsGranted(coreStatsGranted)
        );
    }
}
