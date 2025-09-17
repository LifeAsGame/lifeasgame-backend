package online.lifeasgame.character.presentation;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.PlayerCertificationFacade;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.presentation.mapper.PlayerCertificationWebMapper;
import online.lifeasgame.character.presentation.request.PlayerCertificationRequest;
import online.lifeasgame.character.presentation.response.PlayerCertificationResponse;
import online.lifeasgame.character.presentation.spec.PlayerCertificationApiSpecV1;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.PlayerCertificationInfos>> playerCertificationInfos() {
        List<PlayerCertificationResult.PlayerCertificationInfo> playerCertificationInfos = playerCertificationFacade.getPlayerCertificationInfos();

        return ApiResponses.ok(
                PlayerCertificationWebMapper.toPlayerCertificationInfos(playerCertificationInfos)
        );
    }

    @Override
    @PostMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.CreatedPlayerCertification>> createPlayerCertification(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.CreatePlayerCertification request
    ) {
        PlayerCertificationResult.CreatedPlayerCertification certificationInfo = playerCertificationFacade.createPlayerCertification(
                PlayerCertificationWebMapper.toCommand(certificationId, request)
        );
        return ApiResponses.created(
                URI.create("/api/v1/certifications/"),
                PlayerCertificationWebMapper.toCreatedPlayerCertification(certificationInfo)
        );
    }

    @Override
    @PatchMapping("/certifications/{certificationId}")
    public ResponseEntity<ApiResponse<PlayerCertificationResponse.ChangedPlayerCertification>> updatePlayerCertification(
            @PathVariable Long certificationId,
            @Valid @RequestBody PlayerCertificationRequest.ChangePlayerCertification request
    ) {
        PlayerCertificationResult.ChangedPlayerCertification changedPlayerCertification = playerCertificationFacade.changePlayerCertification(PlayerCertificationWebMapper.toCommand(certificationId, request));
        return ApiResponses.ok(
                PlayerCertificationWebMapper.toChangedPlayerCertification(changedPlayerCertification)
        );
    }
}
