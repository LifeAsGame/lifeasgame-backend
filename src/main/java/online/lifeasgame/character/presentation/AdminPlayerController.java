package online.lifeasgame.character.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.AdminPlayerService;
import online.lifeasgame.character.application.result.AdminPlayerResult;
import online.lifeasgame.character.presentation.mapper.AdminPlayerWebMapper;
import online.lifeasgame.character.presentation.request.AdminPlayerRequest;
import online.lifeasgame.character.presentation.response.AdminPlayerResponse;
import online.lifeasgame.character.presentation.spec.AdminPlayerApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
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
        AdminPlayerResult.ExpGranted expGranted = adminPlayerService.grantExp(playerId, request.expDelta());
        return ApiResponses.ok(
                AdminPlayerWebMapper.toExpGranted(expGranted)
        );
    }

    @Override
    @PatchMapping("/{playerId}/health/current")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentHp>> updateCurrentHp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHp request
    ){
        AdminPlayerResult.CurrentHp currentHp = adminPlayerService.changeHp(AdminPlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                AdminPlayerWebMapper.toCurrentHp(currentHp)
        );
    }

    @Override
    @PatchMapping("/{playerId}/health/capacity")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.HpCapacity>> updateHpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHpCapacity request
    ) {
        AdminPlayerResult.HpCapacity hpCapacity = adminPlayerService.changeHpCapacity(AdminPlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                AdminPlayerWebMapper.toHpCapacity(hpCapacity)
        );
    }

    @Override
    @PatchMapping("/{playerId}/mana/current")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentMp>> updateCurrentMp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMp request
    ){
        AdminPlayerResult.CurrentMp currentMp = adminPlayerService.changeMp(AdminPlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                AdminPlayerWebMapper.toCurrentMp(currentMp)
        );
    }

    @Override
    @PatchMapping("/{playerId}/mana/capacity")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.MpCapacity>> updateMpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMpCapacity request
    ) {
        AdminPlayerResult.MpCapacity mpCapacity = adminPlayerService.changeMpCapacity(AdminPlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                AdminPlayerWebMapper.toMpCapacity(mpCapacity)
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

    @Override
    @PostMapping("/{playerId}/status-effects/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.StatusEffectsGranted>> grantStatusEffects(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantStatusEffects request
    ) {
        AdminPlayerResult.StatusEffectsGranted statusEffectsGranted = adminPlayerService.grantStatusEffects(AdminPlayerWebMapper.toCommand(playerId, request));
        return ApiResponses.ok(
                AdminPlayerWebMapper.toStatusEffectsGranted(statusEffectsGranted)
        );
    }

    @Override
    @PatchMapping("/{playerId}/titles/{titleId}")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.UpdatedTitle>> updateTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    ) {
        AdminPlayerResult.UpdatedTitle updatedTitle = adminPlayerService.changeRepresentativeTitle(playerId, titleId);

        return ApiResponses.ok(
                AdminPlayerWebMapper.toUpdatedTitle(updatedTitle)
        );
    }
}
