package online.lifeasgame.character.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.admin.mapper.AdminPlayerTitleWebMapper;
import online.lifeasgame.character.api.admin.request.AdminPlayerHolderGrantRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;
import online.lifeasgame.character.api.admin.spec.AdminPlayerTitleApiSpecV1;
import online.lifeasgame.character.application.AdminPlayerHolderGrantService;
import online.lifeasgame.character.application.PlayerTitleService;
import online.lifeasgame.character.application.PlayerHolderQueryService;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminPlayerTitleController implements AdminPlayerTitleApiSpecV1 {

    private final AdminPlayerHolderGrantService holderGrantService;
    private final PlayerTitleService playerTitleService;
    private final PlayerHolderQueryService playerHolderQueryService;

    @Override
    @GetMapping("/{playerId}/titles")
    public ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Infos>> getTitles(
            @PathVariable Long playerId
    ) {
        List<PlayerTitleResult.Info> result =
                playerHolderQueryService.getTitleInfos(playerId);
        return ApiResponses.ok(AdminPlayerTitleWebMapper.toInfos(playerId, result));
    }

    @Override
    @PostMapping("/{playerId}/titles/{titleId}")
    public ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Granted>> grantTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody AdminPlayerHolderGrantRequest.Grant request
    ) {
        PlayerTitleResult.Created result = holderGrantService.grantTitle(
                AdminPlayerTitleWebMapper.toGrantCommand(
                        playerId,
                        titleId,
                        request,
                        idempotencyKey,
                        correlationId
                )
        );
        return ApiResponses.ok(AdminPlayerTitleWebMapper.toGrantedTitle(result));
    }

    @Override
    @DeleteMapping("/{playerId}/titles/{titleId}")
    public ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Revoked>> revokeTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    ) {
        PlayerTitleResult.Revoked result = playerTitleService.revokeTitle(playerId, titleId);
        return ApiResponses.deleted(AdminPlayerTitleWebMapper.toRevoked(result));
    }
}
