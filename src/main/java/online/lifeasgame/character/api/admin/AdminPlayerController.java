package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerApiSpecV1;
import online.lifeasgame.character.application.PlayerService;
import online.lifeasgame.character.application.PlayerQueryService;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerController implements AdminPlayerApiSpecV1 {

    private final PlayerService playerService;
    private final PlayerQueryService playerQueryService;

    @Override
    @PostMapping("/{playerId}/exp/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.ExpGranted>> grantExp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantExp request
    ) {
        PlayerResult.ExpGranted result = playerService.grantExp(playerId, request.expDelta());
        return ApiResponses.ok(AdminPlayerWebMapper.toExpGranted(result));
    }

    @Override
    @PatchMapping("/{playerId}/health/current")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentHp>> setCurrentHp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHp request
    ){
        PlayerResult.CurrentHp result =
                playerService.adjustHp(AdminPlayerWebMapper.toChangeHpCommand(playerId, request));
        return ApiResponses.ok(AdminPlayerWebMapper.toCurrentHp(result));
    }

    @Override
    @PatchMapping("/{playerId}/health/capacity")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.HpCapacity>> setHpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHpCapacity request
    ) {
        PlayerResult.HpCapacity result =
                playerService.adjustHpCapacity(AdminPlayerWebMapper.toChangeHpCapacityCommand(playerId, request));
        return ApiResponses.ok(AdminPlayerWebMapper.toHpCapacity(result));
    }

    @Override
    @PatchMapping("/{playerId}/mana/current")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentMp>> setCurrentMp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMp request
    ){
        PlayerResult.CurrentMp result = playerService.adjustMp(AdminPlayerWebMapper.toChangeMpCommand(playerId, request));
        return ApiResponses.ok(AdminPlayerWebMapper.toCurrentMp(result));
    }

    @Override
    @PatchMapping("/{playerId}/mana/capacity")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.MpCapacity>> setMpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMpCapacity request
    ) {
        PlayerResult.MpCapacity result =
                playerService.adjustMpCapacity(AdminPlayerWebMapper.toChangeMpCapacityCommand(playerId, request));
        return ApiResponses.ok(AdminPlayerWebMapper.toMpCapacity(result));
    }

    @Override
    @PostMapping("/{playerId}/core-stats/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.CoreStatsGranted>> grantCoreStats(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantCoreStats request
    ) {
        PlayerResult.CoreStatsGranted result =
                playerService.grantCoreStats(AdminPlayerWebMapper.toGrantCoreStatsCommand(playerId, request));
        return ApiResponses.ok(AdminPlayerWebMapper.toCoreStatsGranted(result));
    }

    @Override
    @PostMapping("/{playerId}/status-effects/grant")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.StatusEffectsGranted>> grantStatusEffects(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantStatusEffects request
    ) {
        PlayerResult.StatusEffectsGranted result = playerService.grantStatusEffects(
                AdminPlayerWebMapper.toGrantStatusEffectsCommand(playerId, request)
        );

        return ApiResponses.ok(AdminPlayerWebMapper.toStatusEffectsGranted(result));
    }

    @Override
    @PatchMapping("/{playerId}/rename")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.Renamed>> rename(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.Rename request
    ) {
        PlayerResult.Renamed result = playerService.rename(playerId, AdminPlayerWebMapper.toRenameCommand(request));
        return ApiResponses.ok(AdminPlayerWebMapper.toRenamed(result));
    }

    @Override
    @PatchMapping("/{playerId}/titles/{titleId}")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.UpdatedTitle>> updateTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    ) {
        PlayerResult.UpdatedTitle result = playerService.changeRepresentativeTitle(playerId, titleId);
        return ApiResponses.ok(AdminPlayerWebMapper.toUpdatedTitle(result));
    }

    @Override
    @GetMapping("/{playerId}")
    public ResponseEntity<ApiResponse<AdminPlayerResponse.PlayerInfo>> get(
            @PathVariable Long playerId
    ) {
        PlayerResult.PlayerInfo result = playerQueryService.getPlayerInfo(playerId);
        return ApiResponses.ok(AdminPlayerWebMapper.toPlayerInfo(result));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminPlayerResponse.PlayerSummary>> getPlayerSummary(
            @RequestParam(required = false) Long userId
    ) {
         PlayerResult.PlayerSummary result = playerQueryService.getPlayerSummary(userId);
        return ApiResponses.ok(AdminPlayerWebMapper.toPlayerSummary(result));
    }
}
