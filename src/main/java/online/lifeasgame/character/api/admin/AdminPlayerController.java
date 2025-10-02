package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerApiSpecV1;
import online.lifeasgame.character.application.PlayerService;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerController implements AdminPlayerApiSpecV1 {

    private final PlayerService adminPlayerService;

    @Override
    @PostMapping("/{playerId}/exp/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.ExpGranted>> grantExp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantExp request
    ) {
        PlayerResult.ExpGranted expGranted = adminPlayerService.grantExp(playerId, request.expDelta());
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
        PlayerResult.CurrentHp currentHp = adminPlayerService.changeHp(AdminPlayerWebMapper.toCommand(playerId, request));
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
        PlayerResult.HpCapacity hpCapacity = adminPlayerService.changeHpCapacity(AdminPlayerWebMapper.toCommand(playerId, request));
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
        PlayerResult.CurrentMp currentMp = adminPlayerService.changeMp(AdminPlayerWebMapper.toCommand(playerId, request));
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
        PlayerResult.MpCapacity mpCapacity = adminPlayerService.changeMpCapacity(AdminPlayerWebMapper.toCommand(playerId, request));
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
        PlayerResult.CoreStatsGranted coreStatsGranted =
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
        PlayerResult.StatusEffectsGranted statusEffectsGranted = adminPlayerService.grantStatusEffects(AdminPlayerWebMapper.toCommand(playerId, request));
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
        PlayerResult.UpdatedTitle updatedTitle = adminPlayerService.changeRepresentativeTitle(playerId, titleId);

        return ApiResponses.ok(
                AdminPlayerWebMapper.toUpdatedTitle(updatedTitle)
        );
    }
}
