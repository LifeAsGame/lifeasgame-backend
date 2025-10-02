package online.lifeasgame.character.api.player;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerCertificationFacade;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.api.player.mapper.PlayerCertificationWebMapper;
import online.lifeasgame.character.api.player.request.PlayerCertificationRequest;
import online.lifeasgame.character.api.player.response.PlayerCertificationResponse;
import online.lifeasgame.character.api.player.spec.PlayerCertificationApiSpecV1;
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
public class PlayerCertificationController implements PlayerCertificationApiSpecV1 {

    private final PlayerCertificationFacade playerCertificationFacade;

    @Override
    @GetMapping("/certifications")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.Infos>> playerCertificationInfos() {
        List<PlayerCertificationResult.Info> infos = playerCertificationFacade.getPlayerCertificationInfos();

        return ApiResponses.ok(
                PlayerCertificationWebMapper.toPlayerCertificationInfos(infos)
        );
    }

    @Override
    @PostMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.Created>> createPlayerCertification(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.Create request
    ) {
        PlayerCertificationResult.Created certificationInfo = playerCertificationFacade.createPlayerCertification(
                PlayerCertificationWebMapper.toCommand(certificationId, request)
        );
        return ApiResponses.created(
                URI.create("/api/v1/certifications"),
                PlayerCertificationWebMapper.toCreatedPlayerCertification(certificationInfo)
        );
    }

    @Override
    @PatchMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.Changed>> updatePlayerCertification(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.Change request
    ) {
        PlayerCertificationResult.Changed changed = playerCertificationFacade.changePlayerCertification(PlayerCertificationWebMapper.toCommand(certificationId, request));
        return ApiResponses.ok(
                PlayerCertificationWebMapper.toChangedPlayerCertification(changed)
        );
    }

    @Override
    @DeleteMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<Long>> deletePlayerCertification(@PathVariable Long certificationId) {
        playerCertificationFacade.deletePlayerCertification(certificationId);
        return ApiResponses.deleted(certificationId);
    }
}
