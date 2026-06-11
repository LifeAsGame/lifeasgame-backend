package online.lifeasgame.character.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.api.player.mapper.PlayerCertificationWebMapper;
import online.lifeasgame.character.api.player.request.PlayerCertificationRequest;
import online.lifeasgame.character.api.player.response.PlayerCertificationResponse;
import online.lifeasgame.character.api.player.spec.PlayerCertificationApiSpecV1;
import online.lifeasgame.character.application.PlayerCertificationFacade;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerCertificationController implements PlayerCertificationApiSpecV1 {

    private final PlayerCertificationFacade playerCertificationFacade;

    @Override
    @GetMapping("/certifications")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.Infos>> playerCertificationInfos() {
        List<PlayerCertificationResult.Info> results = playerCertificationFacade.getPlayerCertificationInfos();
        return ApiResponses.ok(PlayerCertificationWebMapper.toInfos(results));
    }

    @Override
    @PostMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.Created>> create(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.Create request
    ) {
        PlayerCertificationResult.Created result =
                playerCertificationFacade.createPlayerCertification(
                        PlayerCertificationWebMapper.toCreateCommand(certificationId, request)
                );

        return ApiResponses.created(
                URI.create("/api/v1/certifications"),
                PlayerCertificationWebMapper.toCreated(result)
        );
    }

    @Override
    @PatchMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.Changed>> update(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.Update request
    ) {
        PlayerCertificationResult.Changed result =
                playerCertificationFacade.changePlayerCertification(
                        PlayerCertificationWebMapper.toChangeCommand(certificationId, request)
                );

        return ApiResponses.ok(PlayerCertificationWebMapper.toChanged(result));
    }

    @Override
    @DeleteMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<Long>> delete(@PathVariable Long certificationId) {
        playerCertificationFacade.deletePlayerCertification(certificationId);
        return ApiResponses.deleted(certificationId);
    }
}
